package com.samjakob.chunguschat.socket;

public class ChungusProtocol {
    
    public static final int PROTOCOL_VERSION = 3;
    
    
    /**
     * 
     * ChungusProtocol
     * 
     * ------------------------
     * 1. Defined Packets
     * ------------------------
     * 
     * =====================================
     * 
     *          -> Bit Mask: Special Case
     * ---------
     * 0000 0000 - Chat Message
     * 
     * =====================================
     * 
     * xxxx xx1x -> Bit Mask: Basic Client Commands
     * ---------
     * 0000 0010 - Username
     * 0000 0011 - MOTD
     * 
     * =====================================
     * 
     * xxxx x1xx -> Bit Mask: Server Information Commands
     * ---------
     * 0000 0100 - User List
     * 0000 0101 - Set MOTD
     * 
     * =====================================
     * 
     * xxx1 xxxx -> Bit Mask: ChungusChat Internal Protocol
     * ---------
     * 0001 0000 - Connection Initialization Handshake
     * 0001 0001 - System Announcement Message
     * 
     * =====================================
     * 
     * ------------------------
     * 2. Special Features
     * ------------------------
     * 
     * 1xxx xxxx -> Bit Mask: Internal Command
     * 
     * Internal Commands are handled silently by either
     * the client or the server. An example of this is
     * the user list command.
     * 
     * It's non-silent form (0000 0100) is used when the
     * user runs the /list (or /users) command.
     * However, the silent form (1000 0100) is used when
     * the ChungusChat client has requested the user
     * list for the sidebar.
     * 
     */
    
    public static final int CHAT_MESSAGE               = 0b00000000;
    public static final int USERNAME                   = 0b00000010;
    public static final int MOTD                       = 0b00000011;

    public static final int COMMAND_USERS              = 0b00000100;
    public static final int COMMAND_SET_MOTD           = 0b00000101;


    public static final int SYSTEM_HANDSHAKE           = 0b00010000;
    public static final int SYSTEM_ANNOUNCEMENT        = 0b00010001;
    
    public static final int FT_OFFER                   = 0b00100000;
    public static final int FT_ACCEPT                  = 0b00100001;
    
    //                           Command silent by default ↓
    public static final int SECRET_KCC                 = 0b11000000;
    
    public static final int PROTO_MASK_SILENT_COMMAND  = 0b10000000;

}
