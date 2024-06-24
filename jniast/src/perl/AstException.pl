#!/usr/bin/perl -w

#+
#  Name:
#     AstException.pl

#  Purpose:
#     Write java/C source for identifying error status codes.

#  Usage:
#     AstException.pl type-flag name messgenfile

#  Description:
#     This script writes either the java source code for an Exception
#     class, or the C source code for a corresponding function.
#     Together these allow error codes to be accessed by name from 
#     Java source code.

#  Arguments:
#     type-flag
#        Must be either '-c' to write the C source code of a function
#        called 'name' or '-java' to write the java source code of
#        a class called 'name'.
#     name
#        The name of the C function or java class.
#     messgenfile
#        The messgen input file for the error facility in question.

#  Author:
#     MBT: Mark Taylor (Starlink)

#  History:
#     25-SEP-2001 (MBT):
#        Original version.
#-

use strict;

#  Constants.
my( $PackageName ) = "uk.ac.starlink.ast";
my( $ClassName ) = "AstException";
my( $s2i_MethodName ) = "getErrConst";
my( $i2s_MethodName ) = "getErrName";
my( $ErrHeader ) = "ast.h";

#  Set usage string.
my( $self ) = $0;
$self =~ s%.*/%%;
my( $usage ) = "Usage: $self (-c|-java) messgenfile\n";

#  Validate arguments.
if ( @ARGV != 2 ) {
   die( $usage );
}
my( $Typeflag, $Messgenfile ) = @ARGV;
if ( $Typeflag ne "-c" && $Typeflag ne "-java" ) {
   die( $usage );
}

print <<__EOT__;

/* ******************************************************************
 *                         DO NOT EDIT!                             *
 *       This file is autogenerated from AstException.pl            *
 *               and built under Ant control                        *
 ********************************************************************/

__EOT__

if ( $Typeflag eq "-java" ) {
   print <<__EOT__;

package uk.ac.starlink.ast;

/**
 * Thrown to indicate that there has been an AST error of some description.
 * If a call to the underlying AST library occurs results in a non-zero
 * status value, an AstException is thrown.  By calling <code>getStatus</code>
 * on this exception and comparing it with one of the 
 * <code>static final int</code> fields (constants) defined by this class, 
 * it is possible to find out exactly what went wrong.
 * In a few cases an AstException may be thrown by parts of
 * the Java uk.ac.starlink.ast system not provided by the underlying AST 
 * library - in this case the status will be equal to <code>SAI__ERROR</code>.
 *
 * \@author  Mark Taylor (Starlink)
 */
public class $ClassName extends RuntimeException {
    private int status;

    /**
     * Construct an $ClassName.
     *
     * \@param  message  an explanatory message
     */
    public $ClassName( String message ) {
        this( message, SAI__ERROR );
    }

    /**
     * Construct an $ClassName with a given status value.
     *
     * \@param  msgText  an explanatory message
     * \@param  status   the numerical status value
     */
    public AstException( String msgText, int status ) {
        super( msgText + " (" + $i2s_MethodName( status ) + ")" );
        this.status = status;
    }

    /**
     * Get the status value corresponding to this exception.  This ought
     * to correspond to the value of one of this class's static 
     * final member fields.
     *
     * \@return  the error status value
     */
    public int getStatus() {
        return status;
    }

    /**
     * Get the symbolic name corresponding to this exception's status value.
     * This ought to correspond to the name of one of this class's 
     * static final memeber fields.
     *
     * \@return  the error status name
     */
    public String getStatusName() {
        return $i2s_MethodName( status );
    }

    /**
     * Gets the error value for a named constant.
     *
     * \@param  name of the error constant
     * \@return error number
     * \@throws IllegalArgumentException  if no error of that name exists
     */
    private native static int $s2i_MethodName( String ident );

    /**
     * Returns the name of the string that corresponds to a given
     * integer status code.  For instance getStatusName(0)=="SAI__OK".
     *
     * \@param  code
     * \@return symbolic name for error status <code>code</code>
     */
    private native static String $i2s_MethodName( int code );

    /**
     * Status constant for no error.  
     * This value should never be used as the status of an AstException.
     */
    public static final int SAI__OK = ${s2i_MethodName}( "SAI__OK" );

    /**
     * Status constant for unknown error. 
     * This value may be used as the status of an AstException where no
     * value defined in the underlying library is appropriate.
     */
    public static final int SAI__ERROR = ${s2i_MethodName}( "SAI__ERROR" );

__EOT__
}

#  Open the messgen file.
open( MESSGEN, $Messgenfile ) or die( "Failed to open file $Messgenfile\n" );

#  Get the facility name.
my( $line );
$line = <MESSGEN>;
$line =~ /FACILITY *(\S*)/;
my( $fac ) = $1;

#  Read the lines corresponding to error codes.
my( @symbols );
while ( <MESSGEN> ) {
   chomp;
   my( $num, $label, $text ) = split( /,/, $_ );
   my( $symbol ) = "${fac}__${label}";
   push( @symbols, $symbol );
   if ( $Typeflag eq "-java" ) {
      print( "   /** Status constant for error \"$text\" */\n" );
      print( "   public static final int $symbol;\n" );
      print( "   static {\n" );
      print( "       try { $symbol = ${s2i_MethodName}( \"$symbol\" ); }\n" );
      print( "       catch( IllegalArgumentException e ) {\n" );
      print( "           throw new LinkageError(\n" );
      print( "                \"Unknown AST error constant $symbol\" );\n" );
      print( "       }\n" );
      print( "   }\n" );
   }
}

#  Write java footers.
if ( $Typeflag eq "-java" ) {
   print( "}\n" );
   exit( 0 );
}

#  The rest of this script writes the C code.
my( $phname ) = $PackageName;
$phname =~ s/\./_/g;
my( $s2i_funcname ) = "Java_${phname}_${ClassName}_${s2i_MethodName}";
my( $i2s_funcname ) = "Java_${phname}_${ClassName}_${i2s_MethodName}";
print <<__EOT__;
#include <stdlib.h>
#include <string.h>
#include "jni.h"
#include "$ErrHeader"
#include "jniast.h"
#include "sae_par.h"
#include "${phname}_${ClassName}.h"

#define TRY_CONST(Xident) \\
   if ( strcmp( #Xident, ident ) == 0 ) { \\
      result = (jint) Xident; \\
      success = 1; \\
   }

JNIEXPORT jint JNICALL $s2i_funcname(
   JNIEnv *env,          /* Interface pointer */
   jclass class,         /* The class */
   jstring jIdent        /* Name identifying the error constant */
) {
   jint result = (jint) SAI__OK;
   int success = 0;
   const char *ident = (*env)->GetStringUTFChars( env, jIdent, NULL );
   if ( ident != NULL ) {
      TRY_CONST(SAI__OK) 
      else TRY_CONST(SAI__ERROR)
__EOT__
my( $symbol );
foreach $symbol ( @symbols ) {
   print( "      else TRY_CONST($symbol)\n" );
}
print <<__EOT__;
   }
   if ( ! success ) printf( "no such constant %s\\n", ident );
   (*env)->ReleaseStringUTFChars( env, jIdent, ident );
   if ( ! success ) {
      jniastThrowIllegalArgumentException( env, "No such constant" );
   }
   return result;
}
#undef TRY_CONST


#define TRY_CONST(Xident) \\
   case Xident: result = #Xident; break;

JNIEXPORT jstring JNICALL $i2s_funcname(
   JNIEnv *env,          /* Interface pointer */
   jclass class,         /* The class */
   jint jCode            /* Status code */
) {
   const char *result = NULL;

   switch ( jCode ) {
      TRY_CONST(SAI__OK)
      TRY_CONST(SAI__ERROR)
__EOT__
foreach $symbol ( @symbols ) {
   print( "      TRY_CONST($symbol)\n" );
}
print <<__EOT__
   }
   return result ? (*env)->NewStringUTF( env, result ) : NULL;
}
#undef TRY_CONST
__EOT__

# $Id$
