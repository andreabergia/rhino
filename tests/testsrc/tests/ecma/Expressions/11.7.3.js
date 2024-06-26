/* -*- Mode: javascript; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = '11.7.3.js';

/**
   File Name:          11.7.3.js
   ECMA Section:       11.7.3  The unsigned right shift operator ( >>> )
   Description:
   11.7.3 The unsigned right shift operator ( >>> )
   Performs a zero-filling bitwise right shift operation on the left argument
   by the amount specified by the right argument.

   The production ShiftExpression : ShiftExpression >>> AdditiveExpression is
   evaluated as follows:

   1.  Evaluate ShiftExpression.
   2.  Call GetValue(Result(1)).
   3.  Evaluate AdditiveExpression.
   4.  Call GetValue(Result(3)).
   5.  Call ToUint32(Result(2)).
   6.  Call ToUint32(Result(4)).
   7.  Mask out all but the least significant 5 bits of Result(6), that is,
   compute Result(6) & 0x1F.
   8.  Perform zero-filling right shift of Result(5) by Result(7) bits.
   Vacated bits are filled with zero. The result is an unsigned 32 bit
   integer.
   9.  Return Result(8).

   Author:             christine@netscape.com
   Date:               12 november 1997
*/
var SECTION = "11.7.3";
var VERSION = "ECMA_1";
startTest();

writeHeaderToLog( SECTION + "  The unsigned right shift operator ( >>> )");

var addexp = 0;
var power = 0;

for ( power = 0; power <= 32; power++ ) {
  shiftexp = Math.pow( 2, power );

  for ( addexp = 0; addexp <= 32; addexp++ ) {
    new TestCase( SECTION,
                  shiftexp + " >>> " + addexp,
                  UnsignedRightShift( shiftexp, addexp ),
                  shiftexp >>> addexp );
  }
}

test();


function ToInteger( n ) {
  n = Number( n );
  var sign = ( n < 0 ) ? -1 : 1;

  if ( n != n ) {
    return 0;
  }
  if ( Math.abs( n ) == 0 || Math.abs( n ) == Number.POSITIVE_INFINITY ) {
    return n;
  }
  return ( sign * Math.floor(Math.abs(n)) );
}
function ToInt32( n ) {
  n = Number( n );
  var sign = ( n < 0 ) ? -1 : 1;

  if ( Math.abs( n ) == 0 || Math.abs( n ) == Number.POSITIVE_INFINITY) {
    return 0;
  }

  n = (sign * Math.floor( Math.abs(n) )) % Math.pow(2,32);
  n = ( n >= Math.pow(2,31) ) ? n - Math.pow(2,32) : n;

  return ( n );
}
function ToUint32( n ) {
  n = Number( n );
  var sign = ( n < 0 ) ? -1 : 1;

  if ( Math.abs( n ) == 0 || Math.abs( n ) == Number.POSITIVE_INFINITY) {
    return 0;
  }
  n = sign * Math.floor( Math.abs(n) )

    n = n % Math.pow(2,32);

  if ( n < 0 ){
    n += Math.pow(2,32);
  }

  return ( n );
}
function ToUint16( n ) {
  var sign = ( n < 0 ) ? -1 : 1;

  if ( Math.abs( n ) == 0 || Math.abs( n ) == Number.POSITIVE_INFINITY) {
    return 0;
  }

  n = ( sign * Math.floor( Math.abs(n) ) ) % Math.pow(2,16);

  if (n <0) {
    n += Math.pow(2,16);
  }

  return ( n );
}
function Mask( b, n ) {
  b = ToUint32BitString( b );
  b = b.substring( b.length - n );
  b = ToUint32Decimal( b );
  return ( b );
}
function ToUint32BitString( n ) {
  var b = "";
  for ( p = 31; p >=0; p-- ) {
    if ( n >= Math.pow(2,p) ) {
      b += "1";
      n -= Math.pow(2,p);
    } else {
      b += "0";
    }
  }
  return b;
}
function ToInt32BitString( n ) {
  var b = "";
  var sign = ( n < 0 ) ? -1 : 1;

  b += ( sign == 1 ) ? "0" : "1";

  for ( p = 30; p >=0; p-- ) {
    if ( (sign == 1 ) ? sign * n >= Math.pow(2,p) : sign * n > Math.pow(2,p) ) {
      b += ( sign == 1 ) ? "1" : "0";
      n -= sign * Math.pow( 2, p );
    } else {
      b += ( sign == 1 ) ? "0" : "1";
    }
  }

  return b;
}
function ToInt32Decimal( bin ) {
  var r = 0;
  var sign;

  if ( Number(bin.charAt(0)) == 0 ) {
    sign = 1;
    r = 0;
  } else {
    sign = -1;
    r = -(Math.pow(2,31));
  }

  for ( var j = 0; j < 31; j++ ) {
    r += Math.pow( 2, j ) * Number(bin.charAt(31-j));
  }

  return r;
}
function ToUint32Decimal( bin ) {
  var r = 0;


  for ( l = bin.length; l < 32; l++ ) {
    bin = "0" + bin;
  }

  for ( j = 0; j < 32; j++ ) {
    r += Math.pow( 2, j ) * Number(bin.charAt(31-j));

  }

  return r;
}
function RShift( s, a ) {
  s = ToUint32BitString( s );
  for ( z = 0; z < a; z++ ) {
    s = "0" + s;
  }
  s = s.substring( 0, s.length - a );

  return ToUint32Decimal(s);
}
function UnsignedRightShift( s, a ) {
  s = ToUint32( s );
  a = ToUint32( a );
  a = Mask( a, 5 );
  return ( RShift( s, a ) );
}
