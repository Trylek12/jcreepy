
package io.netty.buffer;

import io.netty.buffer.ByteBuf;

public interface ByteBufIndexFinder {
    public static final ByteBufIndexFinder NUL = new ByteBufIndexFinder(){

        @Override
        public boolean find(ByteBuf buffer, int guessedIndex) {
            return buffer.getByte(guessedIndex) == 0;
        }
    };
    public static final ByteBufIndexFinder NOT_NUL = new ByteBufIndexFinder(){

        @Override
        public boolean find(ByteBuf buffer, int guessedIndex) {
            return buffer.getByte(guessedIndex) != 0;
        }
    };
    public static final ByteBufIndexFinder CR = new ByteBufIndexFinder(){

        @Override
        public boolean find(ByteBuf buffer, int guessedIndex) {
            return buffer.getByte(guessedIndex) == 13;
        }
    };
    public static final ByteBufIndexFinder NOT_CR = new ByteBufIndexFinder(){

        @Override
        public boolean find(ByteBuf buffer, int guessedIndex) {
            return buffer.getByte(guessedIndex) != 13;
        }
    };
    public static final ByteBufIndexFinder LF = new ByteBufIndexFinder(){

        @Override
        public boolean find(ByteBuf buffer, int guessedIndex) {
            return buffer.getByte(guessedIndex) == 10;
        }
    };
    public static final ByteBufIndexFinder NOT_LF = new ByteBufIndexFinder(){

        @Override
        public boolean find(ByteBuf buffer, int guessedIndex) {
            return buffer.getByte(guessedIndex) != 10;
        }
    };
    public static final ByteBufIndexFinder CRLF = new ByteBufIndexFinder(){

        @Override
        public boolean find(ByteBuf buffer, int guessedIndex) {
            byte b = buffer.getByte(guessedIndex);
            return b == 13 || b == 10;
        }
    };
    public static final ByteBufIndexFinder NOT_CRLF = new ByteBufIndexFinder(){

        @Override
        public boolean find(ByteBuf buffer, int guessedIndex) {
            byte b = buffer.getByte(guessedIndex);
            return b != 13 && b != 10;
        }
    };
    public static final ByteBufIndexFinder LINEAR_WHITESPACE = new ByteBufIndexFinder(){

        @Override
        public boolean find(ByteBuf buffer, int guessedIndex) {
            byte b = buffer.getByte(guessedIndex);
            return b == 32 || b == 9;
        }
    };
    public static final ByteBufIndexFinder NOT_LINEAR_WHITESPACE = new ByteBufIndexFinder(){

        @Override
        public boolean find(ByteBuf buffer, int guessedIndex) {
            byte b = buffer.getByte(guessedIndex);
            return b != 32 && b != 9;
        }
    };

    public boolean find(ByteBuf var1, int var2);

}

