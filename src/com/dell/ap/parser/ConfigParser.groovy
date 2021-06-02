// =================================================
// Author: drizzt.xia@dell.com
// Description: Yaml parser
// Yaml file parser
// =================================================


package com.dell.ap.parser;

import com.dell.ap.ProjectConfig;
import com.dell.ap.steps.Steps;
import com.dell.ap.steps.Step;

class ConfigParser {

    private static String LATEST = 'latest';
    private static Integer DEFAULT_TIMEOUT = 600;   // 600 seconds

    static ProjectConfig parse(def yaml, def env) {
        ProjectConfig pc = new ProjectConfig();

        // job display name
        pc.name = yaml.name;

        // job description name
        pc.description = yaml.description;

        // parse the environment variables and jenkins environment variables to be passed
        pc.environment = parseEnvironment(yaml.environment, yaml.jenkinsEnvironment, env);

        // parse the execution steps
        pc.steps = parseSteps(yaml.steps);

        pc.timeout = yaml.timeout ?: DEFAULT_TIMEOUT;

        return pc;
    }

    static def parseEnvironment(def environment, def jenkinsEnvironment, def env) {
        def config = [];

        if (environment) {
            config += environment.collect { k, v -> "${k}=${v}"};
        }

        if (jenkinsEnvironment) {
            config += jenkinsEnvironment.collect { k -> "${k}=${env.getProperty(k)}"};
        }

        return config;
    }

    static def parseSteps(def yamlSteps) {
        List<Step> steps = yamlSteps.collect { stepName, params ->
            Step step = new Step(name: stepName, isParallelStep: false, skipError: params.jobSkipError == true ? true : false)
            if (params?.parallel) {
                // parallel step, not support parallel run again
                step.isParallelStep = true
                step.parallel = params.parallel
            } else {
                step.jobName = params.jobName
                step.parameters = params.jobParameters
            }
            return step
        }
        return new Steps(steps: steps);
    }

}
