package jobs.ecom

import utilities.Credentials
import utilities.Environment

public class EcomDeployBase {
    final def envBranches = ['prod-us', 'prod-jp']

    final String environment
    final String region

    EcomDeployBase(String environment, String region) {
        this.environment = environment
        this.region = region
    }

    String getEnvRegion() {
        String shortRegion = 'australia'.equals(region) ? 'au' : region
        return environment + '-' + shortRegion
    }

    String getEffectiveBranch() {
        String ret = 'master'
        String envregion = getEnvRegion()

        if (envregion == 'prod-australia' || envregion == 'prod-au') {
            ret = 'prod-au'
        }

        if ('tst4-us'.equals(envregion)) {
            ret = 'type-system'
        }
        
        if (envBranches.contains(envregion)) {
            ret = envregion
        }
        return ret
    }

    boolean isProd() {
        return environment.startsWith('prod')
    }

    String getBuildBox() {
        return 'deploy-' + region;
    }

    void configureVault(def job) {
        Environment.vaultWrapper(environment, job)
    }
}
