// =================================================
// Author: drizzt.xia@dell.com
// Description: Provide lock to a pipeline
// =================================================


import static com.dell.ap.Precheck.checkScript
import static com.dell.ap.Precheck.checkParam
import groovy.transform.Field

@Field String STEP_NAME = getClass().getName()

/**
 *
 * 功能: 锁定给定的资源
 *
 * @param parameters
 *        parameters.resource 给定的资源名称, 可以是 ip 或则 任意名称, 多个 job 需要互斥的话，可以设定相同的 resource 名
 * @param body
 * @delegate ArrayList
 */
void call(Map parameters = [:], body) {
    String resource = parameters?.resource ?: ''

    if (resource) {
        lock("${resource}") {
            println("[RESOURCE LOCKED]: ${resource}")
            body()
            println("[RESOURCE RELEASED]: ${resource}")
        }
    } else {
        body()
    }
}