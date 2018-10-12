package com.example.user.groupjump;

public final class jumpStats {
    private final String deviceID;
    private final int targetTime;
    private final int tJumpStart;
    private final int tJumpEnd;
    private long dataOffset;

    public jumpStats(String deviceID, int targetTime, int tJumpStart, int tJumpEnd, long dataOffset) {
        this.deviceID = deviceID;
        this.targetTime = targetTime;
        this.tJumpStart = tJumpStart;
        this.tJumpEnd = tJumpEnd;
        this.dataOffset = dataOffset;
    }

    public String getdeviceID(){
        return deviceID;
    }
    public int gettargetTime() {
        return targetTime;
    }
    public int gettJumpStart(){
        return tJumpStart;
    }
    public int gettJumpEnd(){
        return tJumpEnd;
    }
    public long getdataOffset(){
        return dataOffset;
    }
}
