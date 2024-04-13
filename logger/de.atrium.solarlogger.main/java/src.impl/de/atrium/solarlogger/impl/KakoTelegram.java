/*
 * Copyright (c) Papierfabrik Louisenthal GmbH
 *
 * All rights reserved.
 */
package de.atrium.solarlogger.impl;

import java.util.List;
public class KakoTelegram {

    private boolean      start;
    private boolean      reply;
    private String       address;
    private KakoCommand  cmd;
    private int          noe;
    private String       type;
    private int          status;
    private List<String> payload;
    private String       crc;
    private boolean      end;

    public boolean isValid() {
        return hasStart() && isReply() && hasEnd() && !payload.isEmpty();
    }

    public KakoTelegramType getTelegramType() {
        for (KakoTelegramType telegramType : KakoTelegramType.values()) {
            if (telegramType.matches(this)) {
                return telegramType;
            }
        }
        return KakoTelegramType.INVALID;
    }

    @Override
    public String toString() {
        return "KakoTelegram{" +
               "start=" + start +
               ", reply=" + reply +
               ", address='" + address + '\'' +
               ", cmd='" + cmd + '\'' +
               ", noe=" + noe +
               ", type='" + type + '\'' +
               ", status='" + status + '\'' +
               ", payload=" + payload +
               ", crc='" + crc + '\'' +
               ", end=" + end +
               '}';
    }

    public boolean hasStart() {
        return start;
    }

    public void setStart(boolean start) {
        this.start = start;
    }

    public boolean isReply() {
        return reply;
    }

    public void setReply(boolean reply) {
        this.reply = reply;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public KakoCommand getCmd() {
        return cmd;
    }

    public void setCmd(String cmd) {
        if (cmd != null && cmd.length() == 1) {
            this.cmd = KakoCommand.valueOf(cmd.charAt(0));
        }
    }

    public int getNoe() {
        return noe;
    }

    public void setNoe(int noe) {
        this.noe = noe;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public List<String> getPayload() {
        return payload;
    }

    public void setPayload(List<String> payload) {
        this.payload = payload;
    }

    public String getCrc() {
        return crc;
    }

    public void setCrc(String crc) {
        this.crc = crc;
    }

    public boolean hasEnd() {
        return end;
    }

    public void setEnd(boolean end) {
        this.end = end;
    }

    protected String getPayload(int fieldNo) {
        return payload.get(fieldNo);
    }
}
