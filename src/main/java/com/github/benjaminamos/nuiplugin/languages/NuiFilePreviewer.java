/*
 * Copyright 2022 Benjamin Amos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.benjaminamos.nuiplugin.languages;

import com.github.benjaminamos.nuiplugin.classloader.NuiClassLoader;
import com.github.benjaminamos.nuiplugin.nui.AwtCanvasRenderer;
import com.github.benjaminamos.nuiplugin.nui.AwtFont;
import com.github.benjaminamos.nuiplugin.nui.AwtMouseDevice;
import com.github.benjaminamos.nuiplugin.nui.UIElementLoader;
import com.github.benjaminamos.nuiplugin.services.GestaltModuleService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.impl.LoadTextUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.ui.EditorNotifications;
import com.intellij.ui.components.JBLoadingPanel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.concurrency.NonUrgentExecutor;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;
import org.terasology.input.device.MouseDevice;
import org.terasology.input.device.nulldevices.NullKeyboardDevice;
import org.terasology.nui.FocusManagerImpl;
import org.terasology.nui.ScaleMode;
import org.terasology.nui.UIWidget;
import org.terasology.nui.canvas.CanvasImpl;
import org.terasology.nui.skin.UISkin;
import org.terasology.nui.skin.UISkinBuilder;

import javax.swing.JComponent;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeListener;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NuiFilePreviewer implements FileEditor, DocumentListener, DumbService.DumbModeListener {
    private static final Logger LOG = Logger.getInstance(NuiFilePreviewer.class);
    private final Project project;
    private final VirtualFile file;
    private final GestaltModuleService gestaltModuleService;
    private final JBLoadingPanel loadingPanel;
    private final NuiPanel preview;

    public NuiFilePreviewer(Project project, VirtualFile file) {
        this.project = project;
        this.file = file;
        gestaltModuleService = project.getService(GestaltModuleService.class);
        preview = new NuiPanel(gestaltModuleService);
        loadingPanel = new JBLoadingPanel(null, this);
        loadingPanel.stopLoading();
        loadingPanel.setVisible(false);
        preview.add(loadingPanel);
        Document fileDocument = FileDocumentManager.getInstance().getDocument(file);
        fileDocument.addDocumentListener(this);

        project.getMessageBus().connect(this).subscribe(DumbService.DUMB_MODE, this);

        reload();
    }

    private void loadNuiClasses() {
        @Nullable Module fileModule = ModuleUtil.findModuleForFile(file, project);
        if (fileModule == null) {
            return;
        }

        @Nullable Module codeModule = ModuleManager.getInstance(project).findModuleByName(fileModule.getName() + ".main");
        if (codeModule != null) {
            fileModule = codeModule;
        }

        PsiClass uiWidgetClass = JavaPsiFacade.getInstance(project).findClass(UIWidget.class.getName(), GlobalSearchScope.allScope(project));
        if (uiWidgetClass != null) {
            PsiClass[] inherited = ClassInheritorsSearch
                    .search(uiWidgetClass, fileModule.getModuleRuntimeScope(false), true)
                    .toArray(new PsiClass[0]);

            List<Path> classPaths = new ArrayList<>();
            for (VirtualFile outputPath : OrderEnumerator.orderEntries(fileModule).classes().getRoots()) {
                classPaths.add(Path.of(outputPath.getPath().replace("!", "")));
            }

            // TODO: Cache class loaders per module
            NuiClassLoader classLoader = new NuiClassLoader(classPaths);

            for (PsiClass widgetPsiClass : inherited) {
                if (widgetPsiClass.getQualifiedName() == null || widgetPsiClass.getQualifiedName().startsWith("org.terasology.nui.")) {
                    continue;
                }

                try {
                    String moduleId = gestaltModuleService.getModuleIdForFile(widgetPsiClass.getContainingFile().getVirtualFile());
                    if (moduleId == null) {
                        // Assume the "engine" module, for now.
                        moduleId = "engine";
                    }

                    Class<? extends UIWidget> widgetClass = (Class<? extends UIWidget>) classLoader.loadClass(widgetPsiClass.getQualifiedName());
                    gestaltModuleService.getWidgetLibrary().addWidgetClass(moduleId + ":" + widgetClass.getSimpleName(), widgetClass);
                } catch (Throwable t) {
                    t.getMessage();
                }
            }
        }

        gestaltModuleService.invalidateSkins();

        preview.reload(project, file, LoadTextUtil.loadText(file).toString());
    }

    public void reload() {
        ReadAction.nonBlocking(this::loadNuiClasses)
                .inSmartMode(project)
                .submit(NonUrgentExecutor.getInstance());
    }

    /**
     * Returns a component which represents the editor in UI.
     *
     * @return the component used.
     */
    @Override
    public @NotNull JComponent getComponent() {
        return preview;
    }

    /**
     * Returns a component to be focused when the editor is opened.
     */
    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return preview;
    }

    /**
     * Returns editor's name - a string that identifies the editor among others
     * (e.g.: "GUI Designer" for graphical editing and "Text" for textual representation of a GUI form editors).
     */
    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) @NotNull String getName() {
        return "Preview";
    }

    /**
     * Applies given state to the editor.
     *
     * @param state
     */
    @Override
    public void setState(@NotNull FileEditorState state) {
    }

    /**
     * Returns {@code true} when editor's content differs from its source (e.g. a file).
     */
    @Override
    public boolean isModified() {
        return false;
    }

    /**
     * An editor is valid if its contents still exist.
     * For example, an editor displaying the contents of some file stops being valid if the file is deleted.
     * An editor can also become invalid after being disposed of.
     */
    @Override
    public boolean isValid() {
        return true;
    }

    /**
     * Adds specified listener.
     *
     * @param listener (unused)
     */
    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {
    }

    /**
     * Removes specified listener.
     *
     * @param listener (unused)
     */
    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {
    }

    @Override
    public void selectNotify() {
        reload();
    }

    /**
     * The method is optional. Currently, it is used only by the Find Usages subsystem.
     * Expected to return a location of user's focus - a caret or any other form of selection start.
     */
    @Override
    public @Nullable FileEditorLocation getCurrentLocation() {
        return null;
    }

    /**
     * Usually not invoked directly, see class javadoc.
     */
    @Override
    public void dispose() {
    }

    /**
     * @param key (unused)
     * @return a user data value associated with this object. Doesn't require read action.
     */
    @Override
    public <T> @Nullable T getUserData(@NotNull Key<T> key) {
        return null;
    }

    /**
     * Add a new user data value to this object. Doesn't require write action.
     *
     * @param key (unused)
     * @param value (unused)
     */
    @Override
    public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {
    }

    @Override
    public void documentChanged(@NotNull DocumentEvent event) {
        preview.reload(project, file, event.getDocument().getText());
    }

    @Override
    public void enteredDumbMode() {
        loadingPanel.setVisible(true);
        loadingPanel.startLoading();
    }

    @Override
    public void exitDumbMode() {
        loadingPanel.stopLoading();
        loadingPanel.setVisible(false);

        preview.reload(project, file, LoadTextUtil.loadText(file).toString());
    }

    @Nullable
    @Override
    public VirtualFile getFile() {
        return file;
    }

    public Set<Module> getRequiredModules() {
        return preview.getRequiredModules();
    }

    private static final class NuiPanel extends JBPanel<NuiPanel> implements MouseListener, MouseMotionListener {
        private final MouseDevice nuiMouse;
        private final CanvasImpl nuiCanvas;
        private final AwtCanvasRenderer nuiCanvasRenderer;
        private final JTextArea errorLabel;
        private GestaltModuleService gestaltModuleService;
        private final UIElementLoader uiElementLoader;
        private Set<Module> requiredModules = new HashSet<>();
        private long lastUpdateTime;
        private UISkin defaultSkin;
        private UIWidget rootWidget;

        public NuiPanel(GestaltModuleService gestaltModuleService) {
            this.setLayout(new BorderLayout());
            this.setMinimumSize(new Dimension(0, 0));
            this.setOpaque(true);
            this.setVisible(true);
            this.setFocusable(true);
            this.addMouseListener(this);
            this.addMouseMotionListener(this);

            this.gestaltModuleService = gestaltModuleService;
            uiElementLoader = new UIElementLoader(gestaltModuleService);

            errorLabel = new JTextArea("");
            errorLabel.setLineWrap(true);
            errorLabel.setEnabled(false);
            errorLabel.setVisible(false);
            this.add(errorLabel);

            Dimension size = this.getSize();

            UISkin defaultSkin = new UISkinBuilder()
                    .setTextureScaleMode(ScaleMode.STRETCH)
                    .setFont(new AwtFont(JBUI.Fonts.label()))
                    .build();

            nuiMouse = new AwtMouseDevice(this);
            nuiCanvasRenderer = new AwtCanvasRenderer(new Vector2i(size.width, size.height));
            nuiCanvas = new CanvasImpl(nuiCanvasRenderer, new FocusManagerImpl(), new NullKeyboardDevice(),
                    nuiMouse, null, defaultSkin, 100);
        }

        public void reload(Project project, VirtualFile file, String json) {
            requiredModules.clear();
            try {
                uiElementLoader.setModuleContext(gestaltModuleService.getModuleIdForFile(file));
                rootWidget = uiElementLoader.load(json);
                errorLabel.setVisible(false);
                Set<String> missingClasses = uiElementLoader.getMissingClasses();
                for (String missingClass : missingClasses) {
                    PsiClass[] missingClassCandidates = PsiShortNamesCache.getInstance(project)
                            .getClassesByName(missingClass, GlobalSearchScope.projectScope(project));
                    for (PsiClass candidate : missingClassCandidates) {
                        Module candidateModule = ModuleUtil.findModuleForFile(candidate.getContainingFile().getVirtualFile(), project);
                        if (candidateModule != null) {
                            requiredModules.add(candidateModule);
                        }
                    }
                }
                EditorNotifications.getInstance(project).updateNotifications(file);
            } catch (Throwable t) {
                rootWidget = null;
                StringBuilder stacktraceBuilder = new StringBuilder();
                stacktraceBuilder.append(t.getClass().getName());
                stacktraceBuilder.append(": ");
                stacktraceBuilder.append(t.getMessage());
                stacktraceBuilder.append('\n');
                for (StackTraceElement element : t.getStackTrace()) {
                    if (element.getClassName().equals(NuiPanel.class.getName())) {
                        break;
                    }

                    stacktraceBuilder.append("  at ");
                    stacktraceBuilder.append(element.getClassName());
                    stacktraceBuilder.append('.');
                    stacktraceBuilder.append(element.getMethodName());
                    stacktraceBuilder.append('(');
                    stacktraceBuilder.append(element.getFileName());
                    stacktraceBuilder.append(':');
                    stacktraceBuilder.append(element.getLineNumber());
                    stacktraceBuilder.append(')');
                    stacktraceBuilder.append('\n');
                }
                errorLabel.setText(stacktraceBuilder.toString());
                errorLabel.setVisible(true);
            }

            ApplicationManager.getApplication().invokeLater(this::repaint);

            defaultSkin = gestaltModuleService.getDefaultSkin();
        }

        public Set<Module> getRequiredModules() {
            return requiredModules;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (rootWidget != null) {
                nuiCanvasRenderer.setGraphics(g);

                Dimension size = this.getSize();
                nuiCanvasRenderer.setSize(new Vector2i(size.width, size.height));

                nuiCanvas.setGameTime(System.currentTimeMillis());
                nuiCanvas.processMousePosition(nuiMouse.getPosition());

                if (lastUpdateTime == 0) {
                    lastUpdateTime = System.currentTimeMillis();
                }

                try {
                    rootWidget.update((System.currentTimeMillis() - lastUpdateTime) / 1000.0f);
                } catch (Throwable t) {
                    LOG.debug(t);
                }

                nuiCanvas.preRender();

                try {
                    if (defaultSkin != null) {
                        nuiCanvas.setSkin(defaultSkin);
                    }
                    nuiCanvas.drawWidget(rootWidget);
                } catch (Throwable t) {
                    // TODO: Show error?
                    LOG.debug(t);
                }

                nuiCanvas.postRender();

                lastUpdateTime = System.currentTimeMillis();
            }
        }

        @Override
        public void mouseClicked(MouseEvent mouseEvent) {
            repaint();
        }

        @Override
        public void mousePressed(MouseEvent mouseEvent) {
        }

        @Override
        public void mouseReleased(MouseEvent mouseEvent) {
        }

        @Override
        public void mouseEntered(MouseEvent mouseEvent) {
        }

        @Override
        public void mouseExited(MouseEvent mouseEvent) {
        }

        @Override
        public void mouseDragged(MouseEvent mouseEvent) {
            repaint();
        }

        @Override
        public void mouseMoved(MouseEvent mouseEvent) {
            repaint();
        }
    }
}
