// =================================================
// Author: drizzt.xia@dell.com
// Description: pipeline will not exit when error occurred
// =================================================


import static com.dell.ap.Precheck.checkParam

import groovy.transform.Field

@Field String STEP_NAME = getClass().getName()

void call(parameters = [:],  Closure body) {
    def skip = checkParam(this, "skip", parameters.skip, true)

    echo "[DEBUG] Skip Error: ${skip}"
    if (skip) {
        catchError {
            body()
        }
    } else {
        body()
    }

}
