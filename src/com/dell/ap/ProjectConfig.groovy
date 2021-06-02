// =================================================
// Author: drizzt.xia@dell.com
// Description: Yaml parser
// yaml pipeline definition
// =================================================

package com.dell.ap;

import com.dell.ap.steps.Steps;

class ProjectConfig {
    def name;
    def description;
    def environment;
    Steps steps;
    def timeout;
}
