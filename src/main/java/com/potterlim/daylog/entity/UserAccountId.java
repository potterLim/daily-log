package com.potterlim.daylog.entity;

public final class UserAccountId {

    private final long mValue;

    public UserAccountId(long value) {
        if (value <= 0L) {
            throw new IllegalArgumentException("value must be positive.");
        }

        mValue = value;
    }

    public static UserAccountId from(Long valueOrNull) {
        if (valueOrNull == null) {
            throw new IllegalArgumentException("valueOrNull must not be null.");
        }

        return new UserAccountId(valueOrNull);
    }

    public long getValue() {
        return mValue;
    }

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }

        if (!(otherObject instanceof UserAccountId otherUserAccountId)) {
            return false;
        }

        return mValue == otherUserAccountId.mValue;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(mValue);
    }

    @Override
    public String toString() {
        return String.valueOf(mValue);
    }
}
