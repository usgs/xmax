package com.isti.util;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */

public class Math10 {

  public Math10() {
  }

  public static double log10(double num)
  {

    return (Math.log(num)/Math.log(10));

  }

  public static void main(String[] args) {
    Math10 math101 = new Math10();
  }
}