/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.samjakob.chunguschat.socket;

/**
 *
 * @author samuel.mearns
 */
public interface PropertyUpdatedCallback {
    
    void execute(Property property, Object data);
    
    static enum Property {
        USER_LIST;
    }
    
}
