package com.samjakob.chunguschat.socket;

public interface CommandCallback {

    void execute(int command, Object data);

}
