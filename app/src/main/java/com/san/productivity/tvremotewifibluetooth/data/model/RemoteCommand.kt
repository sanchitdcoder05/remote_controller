package com.san.productivity.tvremotewifibluetooth.data.model

enum class RemoteCommand(val samsungKey: String, val label: String) {
    POWER("KEY_POWER", "Power"),
    POWER_OFF("KEY_POWEROFF", "Power Off"),
    
    // Navigation
    UP("KEY_UP", "Up"),
    DOWN("KEY_DOWN", "Down"),
    LEFT("KEY_LEFT", "Left"),
    RIGHT("KEY_RIGHT", "Right"),
    ENTER("KEY_ENTER", "Select"),
    BACK("KEY_RETURN", "Back"),
    HOME("KEY_HOME", "Home"),
    MENU("KEY_MENU", "Menu"),
    SOURCE("KEY_SOURCE", "Source"),
    
    // Volume
    VOLUME_UP("KEY_VOLUP", "Vol +"),
    VOLUME_DOWN("KEY_VOLDOWN", "Vol -"),
    MUTE("KEY_MUTE", "Mute"),
    
    // Channels
    CHANNEL_UP("KEY_CHUP", "Ch +"),
    CHANNEL_DOWN("KEY_CHDOWN", "Ch -"),
    PREV_CHANNEL("KEY_PRECH", "Prev Ch"),
    
    // Media Playback
    PLAY("KEY_PLAY", "Play"),
    PAUSE("KEY_PAUSE", "Pause"),
    STOP("KEY_STOP", "Stop"),
    REWIND("KEY_REWIND", "Rewind"),
    FAST_FORWARD("KEY_FF", "Fast Fwd"),
    
    // System / Utility
    VOICE("KEY_VOICE", "Mic"),
    SEARCH("KEY_SEARCH", "Search"),
    EXIT("KEY_EXIT", "Exit"),
    INFO("KEY_INFO", "Info"),
    GUIDE("KEY_GUIDE", "Guide"),
    SETTINGS("KEY_TOOLS", "Settings"),
    TOOLS("KEY_TOOLS", "Tools"),
    CONTENTS("KEY_CONTENTS", "Apps"),
    
    // Color keys
    RED("KEY_RED", "Red"),
    GREEN("KEY_GREEN", "Green"),
    YELLOW("KEY_YELLOW", "Yellow"),
    BLUE("KEY_CYAN", "Blue"),
    
    // Number Pad
    NUM_0("KEY_0", "0"),
    NUM_1("KEY_1", "1"),
    NUM_2("KEY_2", "2"),
    NUM_3("KEY_3", "3"),
    NUM_4("KEY_4", "4"),
    NUM_5("KEY_5", "5"),
    NUM_6("KEY_6", "6"),
    NUM_7("KEY_7", "7"),
    NUM_8("KEY_8", "8"),
    NUM_9("KEY_9", "9");

    companion object {
        fun fromNumber(digit: Int): RemoteCommand? {
            return when (digit) {
                0 -> NUM_0
                1 -> NUM_1
                2 -> NUM_2
                3 -> NUM_3
                4 -> NUM_4
                5 -> NUM_5
                6 -> NUM_6
                7 -> NUM_7
                8 -> NUM_8
                9 -> NUM_9
                else -> null
            }
        }
    }
}
