package utils;

import com.android.tools.idea.gradle.parser.GradleBuildFile;
import com.android.tools.idea.gradle.project.GradleProjectImporter;
import com.android.tools.idea.gradle.project.GradleSyncListener;
import com.android.tools.idea.gradle.task.AndroidGradleTaskManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrBlockStatement;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrStatement;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;

import java.util.*;

/**
 * Created by pengwei on 16/9/13.
 */
public final class GradleUtil {

    /**
     * gradle sync
     *
     * @param project
     * @param listener
     */
    public static void startSync(Project project, GradleSyncListener listener) {
        GradleProjectImporter.getInstance().requestProjectSync(project, listener);
    }

    /**
     * 执行task
     *
     * @param project
     * @param taskName
     * @param args
     * @param listener
     */
    public static void executeTask(Project project, String taskName, String args, ExternalSystemTaskNotificationListener listener) {
        AndroidGradleTaskManager manager = new AndroidGradleTaskManager();
        List<String> taskNames = new ArrayList<>();
        if (taskName != null) {
            taskNames.add(taskName);
        }
        List<String> vmOptions = new ArrayList<>();
        List<String> params = new ArrayList<>();
        if (args != null) {
            params.add(args);
        }
        if (listener == null) {
            listener = new ExternalSystemTaskNotificationListenerAdapter() {
            };
        }
        manager.executeTasks(
                ExternalSystemTaskId.create(GradleConstants.SYSTEM_ID, ExternalSystemTaskType.EXECUTE_TASK, project),
                taskNames, project.getBasePath(), null, vmOptions, params, null, listener);
    }

    /**
     * 查找所有的build.gradle文件
     *
     * @param project
     * @return
     */
    public static Collection<VirtualFile> getAllGradleFile(Project project) {
        Collection<VirtualFile> collection = FilenameIndex.getVirtualFilesByName(project,
                GradleConstants.DEFAULT_SCRIPT_NAME, GlobalSearchScope.allScope(project));
        return collection == null ? Collections.EMPTY_LIST : collection;
    }

    /**
     * 插入插件的表达式
     * apply plugin: 'com.antfortune.freeline'
     *
     * @param project
     * @param psiFile
     * @param pluginId
     */
    public static void applyPlugin(Project project, GroovyFile psiFile, String pluginId) {
        GroovyPsiElementFactory factory = GroovyPsiElementFactory.getInstance(project);
        GrStatement grStatement = factory.createExpressionFromText(String.format("apply plugin: \'%s\'",
                new Object[]{pluginId}), null);
        GrExpression expression = GroovyFileUil.getLastPlugin(psiFile);
        if (expression != null && expression.getParent() != null) {
            psiFile.addAfter(grStatement, expression.getParent());
            // 换行
            psiFile.addAfter(factory.createLineTerminator("\n"), expression.getParent());
        }
        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
        Document document = documentManager.getDocument(psiFile);
        if (document != null) {
            documentManager.commitDocument(document);
        }
    }

    public static void applyPlugin(Project project, VirtualFile file, String pluginId) {
        GradleBuildFile gradleBuildFile = new GradleBuildFile(file, project);
        if (gradleBuildFile != null) {
            PsiFile psiFile = gradleBuildFile.getPsiFile();
            if (psiFile != null && psiFile instanceof GroovyFile) {
                applyPlugin(project, (GroovyFile) psiFile, pluginId);
            }
        }
    }
}
