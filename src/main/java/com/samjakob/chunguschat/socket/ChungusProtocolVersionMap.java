package com.samjakob.chunguschat.socket;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author samuel.mearns
 */
public enum ChungusProtocolVersionMap {
    
    LEGACY(1, "Legacy"),
    ANTHONY(2, "v1.0"),
    DWEND(3, "v1.1");
    
    private int protocolVersion;
    private String versionName;

    ChungusProtocolVersionMap(int protocolVersion, String versionName){
        this.protocolVersion = protocolVersion;
        this.versionName = versionName;
    }
    
    public int getProtocolVersion(){
        return this.protocolVersion;
    }
    
    public String getVersionName() {
        return this.versionName;
    }
    
    public static ChungusProtocolVersionMap forProtocolVersion(int protocolVersion){
        for(ChungusProtocolVersionMap pv : ChungusProtocolVersionMap.values()){
            if(pv.getProtocolVersion() == protocolVersion){
                return pv;
            }
        }
        
        return null;
    }
    
}
