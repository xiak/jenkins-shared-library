// =================================================
// Author: drizzt.xia@dell.com
// Description: Avamar func
// =================================================


package com.dell.ap

class Avamar implements Serializable {
    private String ip
    private String version
    private String hostnameTemplate
    private String fqdn

    Avamar(String ip, version, template = "av-_R1_-_R2_.xiak.com") {
        this.ip = ip
        this.version = version
        this.hostnameTemplate = template
        this.fqdn = ""
    }

    /**
     * Get Avamar support key
     * @return
     */
    String getSupportKey() {
        def reg = ~/(\w*)\.(\w*)\.\w*/
        def m = this.version =~ reg
        switch (m[0][1]) {
            case "19":
                // Pumbaa
                return "Supp0rtPum6"
            case "18":
                // Labrator
                return "Supp0rtLab8"
            case "7":
                switch (m[0][2]) {
                    case "6":
                        // rooster
                        return "Supp0rtRoo7"
                    case "5":
                        // laguna
                        return "Supp0rtLag6"
                    case "4":
                        // kensington
                        return "Supp0rtKen10"
                    case "3":
                        // julian
                        return "Supp0rtJul6"
                    case "2":
                        // harmony
                        return "Supp0rtHarV1"
                    case "1":
                        // indio
                        return "Supp0rtInd1"
                    default:
                        return null
                }
            default:
                // Hamster
                return "Supp0rtHam7"
        }
    }

    /**
     * Get OS version
     * @return SLES12 or SLES11
     */
    String getOSVersion() {
        def reg = ~/(\w*)\.(\w*)\.\w*/
        def m = this.version =~ reg
        if (Integer.parseInt(m[0][1]) >= 19) {
            if (Integer.parseInt(m[0][2]) > 1) {
                return "SLES12"
            }
        }
        return "SLES11"
    }

    /**
     * get avamar FQDN
     * @return
     */
    String getFQDN() {
        def reg = ~/\.com/
        def m = this.ip =~ reg
        if ( m.getCount() > 0 ) {
            return this.ip
        }
        reg = ~/\w*\.\w*\.(\w*)\.(\w*)/
        m = this.ip =~ reg
        if (m[0][1] == "" || m[0][2] == "") {
            println("Can't parse ip address")
            return ""
        }
        def r1 = m[0][1]
        def r2 = m[0][2]
        def template = this.hostnameTemplate.replaceAll( /_R1_-_R2_/, "$r1-$r2" )

        return template
    }

    /**
     * get avamar host name
     * @return
     */
    String getHostname() {
        if (! this.fqdn) {
            this.fqdn = this.getFQDN()
        }
        def items = this.fqdn.split("\\.")
        return "${items[0]}"
    }

    /**
     * get avamar gateway name
     * @return
     */
    String getGateway() {
        def ip = this.ip.split("\\.")
        return "${ip[0]}.${ip[1]}.${ip[2]}.1"
    }

}
