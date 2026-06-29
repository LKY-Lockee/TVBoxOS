/**
 * Class that represents the .ASS and .SSA subtitle file format
 *
 * <br><br>
 * Copyright (c) 2012 J. David Requejo <br>
 * j[dot]david[dot]requejo[at] Gmail
 * <br><br>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 * <br><br>
 * The above copyright notice and this permission notice shall be included in all copies
 * or substantial portions of the Software.
 * <br><br>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *
 * @author J. David REQUEJO
 *
 */

package com.github.tvbox.osc.subtitle.model;

public class Style {

    private static int styleCounter;
    /* ATTRIBUTES */
    public String iD;
    public String font;
    public String fontSize;
    /**
     * colors are stored as 8 chars long RGBA
     */
    public String color;
    public String backgroundColor;
    public String textAlign = "";
    public boolean italic;
    public boolean bold;
    public boolean underline;

    /**
     * Constructor that receives a String to use a its identifier
     *
     * @param styleName = identifier of this style
     */
    public Style(String styleName) {
        this.iD = styleName;
    }

    /**
     * Constructor that receives a String with the new styleName and a style to
     * copy
     *
     * @param styleName
     * @param style
     */
    public Style(String styleName, Style style) {
        this.iD = styleName;
        this.font = style.font;
        this.fontSize = style.fontSize;
        this.color = style.color;
        this.backgroundColor = style.backgroundColor;
        this.textAlign = style.textAlign;
        this.italic = style.italic;
        this.underline = style.underline;
        this.bold = style.bold;

    }

    /* METHODS */

    /**
     * To get the string containing the hex value to put into color or
     * background color
     *
     * @param format supported: "name", "&HBBGGRR", "&HAABBGGRR",
     *               "decimalCodedBBGGRR", "decimalCodedAABBGGRR"
     * @param value  RRGGBBAA string
     * @return
     */
    public static String getRGBValue(String format, String value) {
        String color = null;
        if (format.equalsIgnoreCase("name")) {
            // standard color format from W3C
            switch (value) {
                case "transparent":
                    color = "00000000";
                    break;
                case "black":
                    color = "000000ff";
                    break;
                case "silver":
                    color = "c0c0c0ff";
                    break;
                case "gray":
                    color = "808080ff";
                    break;
                case "white":
                    color = "ffffffff";
                    break;
                case "maroon":
                    color = "800000ff";
                    break;
                case "red":
                    color = "ff0000ff";
                    break;
                case "purple":
                    color = "800080ff";
                    break;
                case "fuchsia":
                    color = "ff00ffff";
                    break;
                case "magenta":
                    color = "ff00ffff ";
                    break;
                case "green":
                    color = "008000ff";
                    break;
                case "lime":
                    color = "00ff00ff";
                    break;
                case "olive":
                    color = "808000ff";
                    break;
                case "yellow":
                    color = "ffff00ff";
                    break;
                case "navy":
                    color = "000080ff";
                    break;
                case "blue":
                    color = "0000ffff";
                    break;
                case "teal":
                    color = "008080ff";
                    break;
                case "aqua":
                    color = "00ffffff";
                    break;
                case "cyan":
                    color = "00ffffff ";
                    break;
            }
        } else if (format.equalsIgnoreCase("&HBBGGRR")) {
            // hex format from SSA
            color = value.substring(6) +
                    value.charAt(4) +
                    value.charAt(2) +
                    "ff";
        } else if (format.equalsIgnoreCase("&HAABBGGRR")) {
            // hex format from ASS
            color = value.substring(8) +
                    value.charAt(6) +
                    value.charAt(4) +
                    value.charAt(2);
        } else if (format.equalsIgnoreCase("decimalCodedBBGGRR")) {
            // normal format from SSA
            // any missing 0s are filled in
            StringBuilder colorBuilder = new StringBuilder(Integer.toHexString(Integer.parseInt(value)));
            while (colorBuilder.length() < 6)
                colorBuilder.insert(0, "0");
            color = colorBuilder.toString();
            // order is reversed
            color = color.substring(4) + color.substring(2, 4)
                    + color.substring(0, 2) + "ff";
        } else if (format.equalsIgnoreCase("decimalCodedAABBGGRR")) {
            // normal format from ASS
            // any missing 0s are filled in
            StringBuilder colorBuilder = new StringBuilder(Long.toHexString(Long.parseLong(value)));
            while (colorBuilder.length() < 8)
                colorBuilder.insert(0, "0");
            color = colorBuilder.toString();
            // order is reversed
            color = color.substring(6) + color.substring(4, 6)
                    + color.substring(2, 4) + color.substring(0, 2);
        }
        return color;
    }

    public static String defaultID() {
        return "default" + styleCounter++;
    }

}
