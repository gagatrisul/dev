import jenkins.model.* 
import hudson.model.*
 if(manager.logContains(".*Invalid JWT Signature.*")) {
     def job =  Jenkins.getInstance().getItemByFullName("ecom/ecom-${region}-${environment}-deploy") 
     String bv=manager.build.buildVariables.get("BUILD_VERSION")
     def params = (new  StringParameterValue('BUILD_VERSION', bv))
     def future = job.scheduleBuild2(0, new ParametersAction(params))}