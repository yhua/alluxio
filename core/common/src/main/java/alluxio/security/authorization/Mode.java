/*
 * The Alluxio Open Foundation licenses this work under the Apache License, version 2.0
 * (the "License"). You may not use this work except in compliance with the License, which is
 * available at www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied, as more fully set forth in the License.
 *
 * See the NOTICE file distributed with this work for information regarding copyright ownership.
 */

package alluxio.security.authorization;

import alluxio.Configuration;
import alluxio.Constants;
import alluxio.exception.ExceptionMessage;

import com.google.common.base.Preconditions;

import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;

/**
 * POSIX style file/directory access mode.
 */
@NotThreadSafe
public final class Mode {
  /**
   * Mode bits.
   */
  @ThreadSafe
  public enum Bits {
    NONE("---"),
    EXECUTE("--x"),
    WRITE("-w-"),
    WRITE_EXECUTE("-wx"),
    READ("r--"),
    READ_EXECUTE("r-x"),
    READ_WRITE("rw-"),
    ALL("rwx");

    /** String representation of the bits. */
    private final String mString;

    /** Retain reference to the values array. */
    private static final Bits[] SVALS = values();

    Bits(String s) {
      mString = s;
    }

    @Override
    public String toString() {
      return mString;
    }

    /**
     * Checks whether these bits imply the given bits.
     *
     * @param that mode bits
     * @return true when thes bits imply the given bits
     */
    public boolean imply(Bits that) {
      if (that != null) {
        return (ordinal() & that.ordinal()) == that.ordinal();
      }
      return false;
    }

    /**
     * @param that mode bits
     * @return the intersection of thes bits and the given bits
     */
    public Bits and(Bits that) {
      Preconditions.checkNotNull(that);
      return SVALS[ordinal() & that.ordinal()];
    }

    /**
     * @param that mode bits
     * @return the union of thes bits and the given bits
     */
    public Bits or(Bits that) {
      Preconditions.checkNotNull(that);
      return SVALS[ordinal() | that.ordinal()];
    }

    /**
     * @return the complement of these bits
     */
    public Bits not() {
      return SVALS[7 - ordinal()];
    }
  }

  private Bits mOwnerBits;
  private Bits mGroupBits;
  private Bits mOtherBits;

  /**
   * Constructs an instance of {@link Mode} with the given {@link Bits}.
   *
   * @param ownerBits the owner {@link Bits}
   * @param groupBits the group {@link Bits}
   * @param otherBits the other {@link Bits}
   */
  public Mode(Bits ownerBits, Bits groupBits, Bits otherBits) {
    set(ownerBits, groupBits, otherBits);
  }

  /**
   * Constructs an instance of {@link Mode} with the given mode.
   *
   * @param mode the digital representation of a {@link Mode}
   * @see #toShort()
   */
  public Mode(short mode) {
    fromShort(mode);
  }

  /**
   * Copy constructor.
   *
   * @param mode another {@link Mode}
   */
  public Mode(Mode mode) {
    set(mode.mOwnerBits, mode.mGroupBits, mode.mOtherBits);
  }

  /**
   * @return the owner {@link Bits}
   */
  public Bits getOwnerBits() {
    return mOwnerBits;
  }

  /**
   * @param mode the digital representation of a {@link Mode}
   * @return the owner {@link Bits}
   */
  public static Bits extractOwnerBits(short mode) {
    return Bits.values()[(mode >>> 6) & 7];
  }

  /**
   * @return the group {@link Bits}
   */
  public Bits getGroupBits() {
    return mGroupBits;
  }

  /**
   * @param mode the digital representation of a {@link Mode}
   * @return the group {@link Bits}
   */
  public static Bits extractGroupBits(short mode) {
    return Bits.values()[(mode >>> 3) & 7];
  }

  /**
   * @return the other {@link Bits}
   */
  public Bits getOtherBits() {
    return mOtherBits;
  }

  /**
   * @param mode the digital representation of a {@link Mode}
   * @return the other {@link Bits}
   */
  public static Bits extractOtherBits(short mode) {
    return Bits.values()[mode & 7];
  }

  private void set(Bits u, Bits g, Bits o) {
    mOwnerBits = u;
    mGroupBits = g;
    mOtherBits = o;
  }

  /**
   * Sets {@link Mode} bits using a digital representation.
   *
   * @param n the digital representation of a {@link Mode}
   */
  public void fromShort(short n) {
    Bits[] v = Bits.values();
    set(v[(n >>> 6) & 7], v[(n >>> 3) & 7], v[n & 7]);
  }

  /**
   * Encodes the object as a short.
   *
   * @return the digital representation of this {@link Mode}
   */
  public short toShort() {
    int s = (mOwnerBits.ordinal() << 6) | (mGroupBits.ordinal() << 3) | mOtherBits.ordinal();
    return (short) s;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Mode) {
      Mode that = (Mode) obj;
      return mOwnerBits == that.mOwnerBits && mGroupBits == that.mGroupBits
          && mOtherBits == that.mOtherBits;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return toShort();
  }

  @Override
  public String toString() {
    return mOwnerBits.toString() + mGroupBits.toString() + mOtherBits.toString();
  }

  /**
   * Creates a new mode by applying the given umask {@link Mode} to this mode.
   *
   * @param umask the umask to apply
   * @return a new {@link Mode}
   */
  public Mode applyUMask(Mode umask) {
    return new Mode(mOwnerBits.and(umask.mOwnerBits.not()), mGroupBits.and(umask.mGroupBits.not()),
        mOtherBits.and(umask.mOtherBits.not()));
  }

  /**
   * Creates a new mode by applying the umask specified in configuration to this mode.
   *
   * @param conf the configuration to read the umask from
   * @return a new {@link Mode}
   */
  public Mode applyUMask(Configuration conf) {
    return applyUMask(getUMask(conf));
  }

  /**
   * Gets the default mode.
   *
   * @return the default {@link Mode}
   */
  public static Mode getDefault() {
    return new Mode(Constants.DEFAULT_FILE_SYSTEM_MODE);
  }

  /**
   * Creates the "no access" mode.
   *
   * @return the none {@link Mode}
   */
  public static Mode createNoAccess() {
    return new Mode(Bits.NONE, Bits.NONE, Bits.NONE);
  }

  /**
   * Creates the "full access" mode.
   *
   * @return the none {@link Mode}
   */
  public static Mode createFullAccess() {
    return new Mode(Bits.ALL, Bits.ALL, Bits.ALL);
  }

  /**
   * Gets the file / directory creation umask.
   *
   * @param conf the runtime configuration of Alluxio
   * @return the umask {@link Mode}
   */
  public static Mode getUMask(Configuration conf) {
    int umask = Constants.DEFAULT_FILE_SYSTEM_UMASK;
    if (conf != null) {
      String confUmask = conf.get(Constants.SECURITY_AUTHORIZATION_PERMISSION_UMASK);
      if (confUmask != null) {
        if ((confUmask.length() > 4) || !tryParseInt(confUmask)) {
          throw new IllegalArgumentException(ExceptionMessage.INVALID_CONFIGURATION_VALUE
              .getMessage(confUmask, Constants.SECURITY_AUTHORIZATION_PERMISSION_UMASK));
        }
        int newUmask = 0;
        int lastIndex = confUmask.length() - 1;
        for (int i = 0; i <= lastIndex; i++) {
          newUmask += (confUmask.charAt(i) - '0') << 3 * (lastIndex - i);
        }
        umask = newUmask;
      }
    }
    return new Mode((short) umask);
  }

  private static boolean tryParseInt(String value) {
    try {
      Integer.parseInt(value);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }
}
