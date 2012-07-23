package skyview.util;

/** this class allows you to build an array as you go. */
public class SmartIntArray {

   int sp = 0; // "stack pointer" to keep track of position in the array
   private int[] array;
   private int growthSize;

   public SmartIntArray() {
      this( 1024 );  //default initial size
   }

   public SmartIntArray( int initialSize ) {
      this( initialSize, (int)( initialSize / 4 ) );  //set default growth size
   }

   public SmartIntArray( int initialSize, int growthSize ) {
      this.growthSize = growthSize;
      array = new int[ initialSize ];
   }

   /** add element (integer) to array , check size and increase it if necessary
     * @param i integer to add to array
     */
   public void add( int i ) {
      if( sp >= array.length ) {   // time to grow! 
         int[] tmpArray = new int[ array.length + growthSize ];
         System.arraycopy( array, 0, tmpArray, 0, array.length );
         array = tmpArray;
      }
      array[ sp ] = i;
      sp += 1;
   }

   /** trim array before return 
     * @return trimmed array of integers
     */
   public int[] toArray() {
      int[] trimmedArray = new int[ sp ];
      System.arraycopy( array, 0, trimmedArray, 0, trimmedArray.length ); 
      return trimmedArray;
   }
}
