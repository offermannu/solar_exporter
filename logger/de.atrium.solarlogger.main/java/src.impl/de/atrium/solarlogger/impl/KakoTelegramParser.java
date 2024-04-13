/*
 * Copyright (c) Papierfabrik Louisenthal GmbH
 *
 * All rights reserved.
 */
package de.atrium.solarlogger.impl;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.zfabrik.util.html.Escaper.escapeToJS;

public class KakoTelegramParser {
    private final char[] chars;
    private       int    offset;

    public KakoTelegramParser(String s) {
        this.chars = s.toCharArray();
    }

    public KakoTelegram parse() throws ParseException {
        KakoTelegram kt = new KakoTelegram();
        chr('\n', kt::setStart);                                  // LF start
        chr('*', kt::setReply);                                   // * reply
        address(kt::setAddress);                                  // ADDR
        command(kt::setCmd);                                      // CMD
        spc();

        try {
            switch (kt.getCmd()) {
                case STATUS:
                    // status
                    integer(kt::setNoe);                            // NOE (number of elements)
                    spc();
                    text(kt::setType);                              // TYPE
                    spc();
                    integer(kt::setStatus);                         // STATUS
                    spc();
                    payload(kt.getNoe() - 3, kt::setPayload);       // PAYLOAD - NOE = [NOE] + [TYP] + [STA] + [PYL] => skip 3!
                    spc();
                    crc(kt::setCrc);                                // CRC
                    chr('\r', kt::setEnd);                          // CR end
                    return kt;

                case TOTAL_YIELD:
                    // total yield has 7 payload fields but this is not mentioned in the telegram!
                    kt.setNoe(7);
                    payload(kt.getNoe(), kt::setPayload);           // PAYLOAD
                    chr('\r', kt::setEnd);                          // CR end

                    // telegram contains neither STATUS nor TYPE nor CRC
                    kt.setStatus(0);
                    kt.setType("");
                    kt.setCrc("");
                    return kt;

            }
        } catch (ParseException e) {
            int max = Math.min(offset, chars.length - 1);
            throw new ParseException(
                    e.getMessage() + ": \"" + escapeToJS(new String(chars, 0, max)) +
                    "\". Found '" + escapeToJS(new String(chars, max, 1)) + "' instead!"
                    , e.getErrorOffset());
        }

        throw new ParseException(new String(chars), offset);
    }

    private void chr(char expected, Consumer<Boolean> booleanConsumer) throws ParseException {
        boolean isExcepted = offset < chars.length && chars[offset] == expected;
        booleanConsumer.accept(isExcepted);
        if (isExcepted) {
            offset++;
        } else {
            throw new ParseException("'" + escapeToJS("" + expected) + "' expected", offset);
        }
    }

    private void text(Consumer<String> tokenConsumer) throws ParseException {
        text(ch -> !Character.isWhitespace(ch), tokenConsumer);
    }

    private void text(Predicate<Character> predicate, Consumer<String> tokenConsumer) throws ParseException {

        int pos=offset;
        if (offset < chars.length && predicate.test(chars[pos])) {
            // >= 1 match
            while (++pos < chars.length && predicate.test(chars[pos])) {}
            tokenConsumer.accept(new String(chars, offset, pos-offset));
            offset = pos;

        } else {
            throw new ParseException("Token expected", offset);
        }
    }

    private void command(Consumer<String> cmdConsumer) throws ParseException {
        if (offset < chars.length && Character.isLetterOrDigit(chars[offset])) {
            cmdConsumer.accept(new String(chars, offset++, 1));
        } else {
            throw new ParseException("Command expected", offset);
        }
    }

    private void integer(Consumer<Integer> integerConsumer) throws ParseException {
        try {
            text(Character::isDigit, s -> integerConsumer.accept(Integer.parseInt(s)));
        } catch (NumberFormatException e) {
            throw new ParseException("Integer expected", offset);
        }
    }

    private void address(Consumer<String> tokenConsumer) throws ParseException {
        if (offset+1 < chars.length && Character.isDigit(chars[offset]) && Character.isDigit(chars[offset + 1])) {
            tokenConsumer.accept(new String(chars, offset, 2));
            offset += 2;
        } else {
            throw new ParseException("Address expected", offset);
        }
    }

    private void payload(int count, Consumer<List<String>> payloadConsumer) throws ParseException {
        List<String> payload = new ArrayList<>();

        text(payload::add);
        for (int i = 1; i < count; i++) {
            spc();
            text(payload::add);
        }
        payloadConsumer.accept(payload);
    }

    private void crc(Consumer<String> crcConsumer) throws ParseException {
        text(Character::isLetterOrDigit, crcConsumer);
    }

    // one or more spaces
    private void spc() throws ParseException {
        // min 1 spaces
        if (offset < chars.length && chars[offset] == ' ') {
            // but can be more...
            while (++offset < chars.length && chars[offset] == ' ');

        } else {
            throw new ParseException("Whitespace expected", offset);
        }
    }

    public static void main(String[] args) throws ParseException {
        System.out.println(
                new KakoTelegramParser(
                        "\n*02n 20 100TL 4  555.2  3.78  2101  525.4  1.44   762  237.2  4.32  238.8  4.34  237.1  4.30  2864  2929 0.998i  37.9  " +
                        "22945 A3A1\r"
                ).parse()
        );

        System.out.println(
                new KakoTelegramParser(
                        "\n*043  5349  15250 30058828 30058828 000012:38 009711:14 009711:14\r"
                ).parse()
        );
    }

}
