package org.aion4j.avm.idea.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import org.aion4j.avm.idea.action.ui.DeployArgsUI;
import org.aion4j.avm.idea.misc.PsiCustomUtil;
import org.aion4j.avm.idea.service.AvmCacheService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenProject;

import java.util.HashMap;
import java.util.Map;

public class DeployArgsHelper {

    public static Map<String, String> getAndSaveDeploymentArgs(@NotNull AnActionEvent e, Project project, boolean checkDontShow) {

        Map<String, String> resultArgs = new HashMap<>();

        AvmCacheService avmCacheService = ServiceManager.getService(project, AvmCacheService.class);

        MavenProject mavenProject = PsiCustomUtil.getMavenProject(project, e);

        boolean isAggregatorProject = false;
        String moduleName = null;
        if (mavenProject != null) {
            moduleName = PsiCustomUtil.getMavenProjectName(mavenProject);
            isAggregatorProject = mavenProject.isAggregator() && mavenProject.getModulePaths().size() > 0;
        } else {
            Module module = PsiCustomUtil.getModuleFromAction(project, e);
            moduleName = module != null ? module.getName() : null;//get module nmae
        }

        if (avmCacheService != null) {
            if ((checkDontShow && avmCacheService.shouldNotAskDeployArgs(moduleName)) || isAggregatorProject) { //If called during deployment, it may ignore the UI show

                if (isAggregatorProject) { //check if its top level project
                    resultArgs.putAll(avmCacheService.getAllDeployArgsWithModuleName());
                } else {
                    String deployArgs = avmCacheService.getDeployArgs(moduleName);
                    resultArgs.put("args", deployArgs != null ? deployArgs : "");
                }
            } else {
                DeployArgsUI dialog = new DeployArgsUI(project, moduleName);

                String cacheArgs = avmCacheService.getDeployArgs(moduleName);
                boolean cacheDontAsk = avmCacheService.shouldNotAskDeployArgs(moduleName);

                if (cacheArgs != null)
                    dialog.setDeploymentArgs(cacheArgs);

                dialog.setDontAskSelected(cacheDontAsk);

                boolean result = dialog.showAndGet();
                if (result) {
                    String deployArgs = dialog.getDeploymentArgs();

                    avmCacheService.updateDeployArgs(moduleName, deployArgs);
                    avmCacheService.setShouldNotAskDeployArgs(moduleName, dialog.isDontAskSelected());

                    deployArgs = avmCacheService.getDeployArgs(moduleName);
                    resultArgs.put("args", deployArgs != null ? deployArgs : "");

                }
            }
        }

        return resultArgs;
    }

}