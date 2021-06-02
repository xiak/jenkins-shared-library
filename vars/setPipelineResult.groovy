// =================================================
// Author: drizzt.xia@dell.com
// Description: Set pipeline result
// =================================================


void call(currentBuild, result = 'SUCCESS') {
    echo "Current build result is ${currentBuild.result}, setting it to ${result}."
    currentBuild.result = result
}
