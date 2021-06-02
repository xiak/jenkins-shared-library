// Copyright 2021 Dell - Avamar Authors
// Maintainer: drizzt.xia@dell.com
// Description: pipeline step

package com.dell.ap.steps;

class Step {
    // job name and job parameters
    String jobName
    Map parameters = [:];

    // Parallel run
    boolean isParallelStep;
    Map parallel = [:];
    // step name
    String name;

    // If true, step will not exit when error occurred
    boolean skipError;
}
