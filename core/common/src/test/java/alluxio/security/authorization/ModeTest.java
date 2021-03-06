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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests the {@link Mode} class.
 */
public final class ModeTest {

  /**
   * The exception expected to be thrown.
   */
  @Rule
  public ExpectedException mThrown = ExpectedException.none();

  private Configuration mConf = new Configuration();

  /**
   * Tests the {@link Mode#toShort()} method.
   */
  @Test
  public void toShortTest() {
    Mode mode = new Mode(Mode.Bits.ALL, Mode.Bits.READ_EXECUTE, Mode.Bits.READ_EXECUTE);
    Assert.assertEquals(0755, mode.toShort());

    mode = Mode.getDefault();
    Assert.assertEquals(0777, mode.toShort());

    mode = new Mode(Mode.Bits.READ_WRITE, Mode.Bits.READ, Mode.Bits.READ);
    Assert.assertEquals(0644, mode.toShort());
  }

  /**
   * Tests the {@link Mode#fromShort(short)} method.
   */
  @Test
  public void fromShortTest() {
    Mode mode = new Mode((short) 0777);
    Assert.assertEquals(Mode.Bits.ALL, mode.getOwnerBits());
    Assert.assertEquals(Mode.Bits.ALL, mode.getGroupBits());
    Assert.assertEquals(Mode.Bits.ALL, mode.getOtherBits());

    mode = new Mode((short) 0644);
    Assert.assertEquals(Mode.Bits.READ_WRITE, mode.getOwnerBits());
    Assert.assertEquals(Mode.Bits.READ, mode.getGroupBits());
    Assert.assertEquals(Mode.Bits.READ, mode.getOtherBits());

    mode = new Mode((short) 0755);
    Assert.assertEquals(Mode.Bits.ALL, mode.getOwnerBits());
    Assert.assertEquals(Mode.Bits.READ_EXECUTE, mode.getGroupBits());
    Assert.assertEquals(Mode.Bits.READ_EXECUTE, mode.getOtherBits());
  }

  /**
   * Tests the {@link Mode#Mode(Mode)} constructor.
   */
  @Test
  public void copyConstructorTest() {
    Mode mode = new Mode(Mode.getDefault());
    Assert.assertEquals(Mode.Bits.ALL, mode.getOwnerBits());
    Assert.assertEquals(Mode.Bits.ALL, mode.getGroupBits());
    Assert.assertEquals(Mode.Bits.ALL, mode.getOtherBits());
    Assert.assertEquals(0777, mode.toShort());
  }

  /**
   * Tests the {@link Mode#createNoAccess()} method.
   */
  @Test
  public void createNoAccessTest() {
    Mode mode = Mode.createNoAccess();
    Assert.assertEquals(Mode.Bits.NONE, mode.getOwnerBits());
    Assert.assertEquals(Mode.Bits.NONE, mode.getGroupBits());
    Assert.assertEquals(Mode.Bits.NONE, mode.getOtherBits());
    Assert.assertEquals(0000, mode.toShort());
  }

  /**
   * Tests the {@link Mode#equals(Object)} method.
   */
  @Test
  public void equalsTest() {
    Mode allAccess = new Mode((short) 0777);
    Assert.assertTrue(allAccess.equals(Mode.getDefault()));
    Mode noAccess = new Mode((short) 0000);
    Assert.assertTrue(noAccess.equals(Mode.createNoAccess()));
    Assert.assertFalse(allAccess.equals(noAccess));
  }

  /**
   * Tests the {@link Mode#toString()} method.
   */
  @Test
  public void toStringTest() {
    Assert.assertEquals("rwxrwxrwx", new Mode((short) 0777).toString());
    Assert.assertEquals("rw-r-----", new Mode((short) 0640).toString());
    Assert.assertEquals("rw-------", new Mode((short) 0600).toString());
    Assert.assertEquals("---------", new Mode((short) 0000).toString());
  }

  /**
   * Tests the {@link Mode#getUMask(Configuration)} and
   * {@link Mode#applyUMask(Mode)} methods.
   */
  @Test
  public void umaskTest() {
    String umask = "0022";
    mConf.set(Constants.SECURITY_AUTHORIZATION_PERMISSION_UMASK, umask);
    Mode umaskMode = Mode.getUMask(mConf);
    // after umask 0022, 0777 should change to 0755
    Mode mode = Mode.getDefault().applyUMask(umaskMode);
    Assert.assertEquals(Mode.Bits.ALL, mode.getOwnerBits());
    Assert.assertEquals(Mode.Bits.READ_EXECUTE, mode.getGroupBits());
    Assert.assertEquals(Mode.Bits.READ_EXECUTE, mode.getOtherBits());
    Assert.assertEquals(0755, mode.toShort());
  }

  /**
   * Tests the {@link Mode#getUMask(Configuration)} method to thrown an exception
   * when it exceeds the length.
   */
  @Test
  public void umaskExceedLengthTest() {
    String umask = "00022";
    mConf.set(Constants.SECURITY_AUTHORIZATION_PERMISSION_UMASK, umask);
    mThrown.expect(IllegalArgumentException.class);
    mThrown.expectMessage(ExceptionMessage.INVALID_CONFIGURATION_VALUE.getMessage(umask,
        Constants.SECURITY_AUTHORIZATION_PERMISSION_UMASK));
    Mode.getUMask(mConf);
  }

  /**
   * Tests the {@link Mode#getUMask(Configuration)} method to thrown an exception
   * when it is not an integer.
   */
  @Test
  public void umaskNotIntegerTest() {
    String umask = "NotInteger";
    mConf.set(Constants.SECURITY_AUTHORIZATION_PERMISSION_UMASK, umask);
    mThrown.expect(IllegalArgumentException.class);
    mThrown.expectMessage(ExceptionMessage.INVALID_CONFIGURATION_VALUE.getMessage(umask,
        Constants.SECURITY_AUTHORIZATION_PERMISSION_UMASK));
    Mode.getUMask(mConf);
  }
}
