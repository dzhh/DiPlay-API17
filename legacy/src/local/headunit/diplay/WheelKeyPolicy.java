package local.headunit.diplay;
/** Exact envelopes measured on MX9A0; no guessed volume or long-press mappings. */
public final class WheelKeyPolicy {
 private final long[] at={-10000,-10000,-10000,-10000,-10000};
 public static int decode(byte[] b){if(b==null||b.length!=3||b[0]!=4||b[1]!=1)return 0;switch(b[2]&255){case 4:return 1;case 3:return 2;case 94:return 3;case 95:return 4;default:return 0;}}
 public synchronized boolean accept(int action,long now){if(action<1||action>4)return false;if(now>=at[action]&&now-at[action]<120)return false;at[action]=now;return true;}
 public synchronized boolean duplicateAndroid(int key,long now){int action=key==88?1:key==87?2:0;return action!=0&&now>=at[action]&&now-at[action]<900;}
}
