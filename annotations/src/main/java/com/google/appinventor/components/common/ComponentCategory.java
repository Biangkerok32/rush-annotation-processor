// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2017 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

// This file has been modified by Shreyash Saitwal to add support for extensions
// built with Rush build tool (https://github.com/ShreyashSaitwal/rush-cli)

package com.google.appinventor.components.common;

import java.util.HashMap;
import java.util.Map;

public enum ComponentCategory {
  // TODO(user): i18n category names
  USERINTERFACE("User Interface"),
  LAYOUT("Layout"),
  MEDIA("Media"),
  ANIMATION("Drawing and Animation"),
  MAPS("Maps"),
  SENSORS("Sensors"),
  SOCIAL("Social"),
  STORAGE("Storage"),
  CONNECTIVITY("Connectivity"),
  LEGOMINDSTORMS("LEGO\u00AE MINDSTORMS\u00AE"),
  EXPERIMENTAL("Experimental"),
  EXTENSION("Extension"),
  INTERNAL("For internal use only"),
  // UNINITIALIZED is used as a default value so Swing libraries can still compile
  UNINITIALIZED("Uninitialized");


  // Mapping of component categories to names consisting only of lower-case letters,
  // suitable for appearing in URLs.
  private static final Map<String, String> DOC_MAP = new HashMap<String, String>();
  static {
    DOC_MAP.put("User Interface", "userinterface");
    DOC_MAP.put("Layout", "layout");
    DOC_MAP.put("Media", "media");
    DOC_MAP.put("Drawing and Animation", "animation");
    DOC_MAP.put("Maps", "maps");
    DOC_MAP.put("Sensors", "sensors");
    DOC_MAP.put("Social", "social");
    DOC_MAP.put("Storage", "storage");
    DOC_MAP.put("Connectivity", "connectivity");
    DOC_MAP.put("LEGO\u00AE MINDSTORMS\u00AE", "legomindstorms");
    DOC_MAP.put("Experimental", "experimental");
    DOC_MAP.put("Extension", "extension");
  }


  private String name;

  private ComponentCategory(String categoryName) {
    name = categoryName;
  }

  /**
   * Returns the display name of this category, as used on the Designer palette, such
   * as "Not ready for prime time".  To get the enum name (such as "EXPERIMENTAL"),
   * use {@link #toString}.
   *
   * @return the display name of this category
   */
  public String getName() {
    return name;
  }

  /**
   * Returns a version of the name of this category consisting of only lower-case
   * letters, meant for use in a URL.  For example, for the category with the enum
   * name "EXPERIMENTAL" and display name "Not ready for prime time", this returns
   * "experimental".
   *
   * @return a name for this category consisting of only lower-case letters
   */
  public String getDocName() {
    return DOC_MAP.get(name);
  }
}
