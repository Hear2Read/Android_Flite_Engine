###########################################################################
##                                                                       ##
##                  Language Technologies Institute                      ##
##                     Carnegie Mellon University                        ##
##                         Copyright (c) 2012                            ##
##                        All Rights Reserved.                           ##
##                                                                       ##
##  Permission is hereby granted, free of charge, to use and distribute  ##
##  this software and its documentation without restriction, including   ##
##  without limitation the rights to use, copy, modify, merge, publish,  ##
##  distribute, sublicense, and/or sell copies of this work, and to      ##
##  permit persons to whom this work is furnished to do so, subject to   ##
##  the following conditions:                                            ##
##   1. The code must retain the above copyright notice, this list of    ##
##      conditions and the following disclaimer.                         ##
##   2. Any modifications must be clearly marked as such.                ##
##   3. Original authors' names are not deleted.                         ##
##   4. The authors' names are not used to endorse or promote products   ##
##      derived from this software without specific prior written        ##
##      permission.                                                      ##
##                                                                       ##
##  CARNEGIE MELLON UNIVERSITY AND THE CONTRIBUTORS TO THIS WORK         ##
##  DISCLAIM ALL WARRANTIES WITH REGARD TO THIS SOFTWARE, INCLUDING      ##
##  ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS, IN NO EVENT   ##
##  SHALL CARNEGIE MELLON UNIVERSITY NOR THE CONTRIBUTORS BE LIABLE      ##
##  FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES    ##
##  WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN   ##
##  AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION,          ##
##  ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF       ##
##  THIS SOFTWARE.                                                       ##
##                                                                       ##
###########################################################################
##                                                                       ##
##  Author: Alok Parlikar (aup@cs.cmu.edu)                               ##
##  Date  : July 2012                                                    ##
###########################################################################
##                                                                       ##
## Makefile for Android NDK                                              ##
##                                                                       ##
###########################################################################

LOCAL_PATH:= $(call my-dir)
###########################################################################
# Setup Flite related paths
# We require that FLITEDIR be defined - CORRECT IT OR SET IN THE ENVIRONMENT!
#FLITEDIR := C:/Users/greg/AppData/Local/lxss/home/greg/flite
#FLITEDIR=/home/sushant/workspace/hear2read/trial/flite_android_64bit/flite
FLITEDIR=/home/sushant/workspace/hear2read/trial/flite_092120/flite

ifndef FLITEDIR
  $(error "FLITEDIR variable should be set to path where flite is compiled")
endif

FLITE_BUILD_SUBDIR:=$(TARGET_ARCH_ABI)

# Adding support to arm64-v8, Shyam, 2018/03/07
ifeq "$(TARGET_ARCH_ABI)" "armeabi-v7a"
  FLITE_BUILD_SUBDIR:=armeabiv7a
else ifeq "$(TARGET_ARCH_ABI)" "arm64-v8a"
  FLITE_BUILD_SUBDIR:=armv8
endif

FLITE_LIB_DIR:= $(FLITEDIR)/build/$(FLITE_BUILD_SUBDIR)-android/lib
###########################################################################

include $(CLEAR_VARS)

LOCAL_MODULE    := ttsflite

LOCAL_CPP_EXTENSION := .cc

LOCAL_SRC_FILES := org_hear2read_indic_service.cc \
	org_hear2read_indic_engine.cc \
	org_hear2read_indic_voices.cc \
	org_hear2read_string.cc

LOCAL_STATIC_LIBRARIES := cmu-indic-lex-prebuilt \
                          cmu-indic-lang-prebuilt \
                          cmulex-prebuilt \
                          usenglish-prebuilt \
                          flite-prebuilt

LOCAL_C_INCLUDES := $(FLITEDIR)/include

LOCAL_LDLIBS:= -llog

ifeq ("$(APP_OPTIM)", "debug")
  LOCAL_CFLAGS += -DFLITE_DEBUG_ENABLED=1
else
  LOCAL_CFLAGS += -DFLITE_DEBUG_ENABLED=0
endif

# LOCAL_CFLAGS += -D__ANDROID_API__=$API Commenting this out, already defined - Shyam, 2018/05/30

include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := cmu-indic-lex-prebuilt
LOCAL_SRC_FILES := $(FLITE_LIB_DIR)/libflite_cmu_indic_lex.a
LOCAL_EXPORT_C_INCLUDES := $(FLITEDIR)/include
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := cmu-indic-lang-prebuilt
LOCAL_SRC_FILES := $(FLITE_LIB_DIR)/libflite_cmu_indic_lang.a
LOCAL_EXPORT_C_INCLUDES := $(FLITEDIR)/include
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := cmulex-prebuilt
LOCAL_SRC_FILES := $(FLITE_LIB_DIR)/libflite_cmulex.a
LOCAL_EXPORT_C_INCLUDES := $(FLITEDIR)/include
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := usenglish-prebuilt
LOCAL_SRC_FILES := $(FLITE_LIB_DIR)/libflite_usenglish.a
LOCAL_EXPORT_C_INCLUDES := $(FLITEDIR)/include
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := flite-prebuilt
LOCAL_SRC_FILES := $(FLITE_LIB_DIR)/libflite.a
LOCAL_EXPORT_C_INCLUDES := $(FLITEDIR)/include
include $(PREBUILT_STATIC_LIBRARY)
