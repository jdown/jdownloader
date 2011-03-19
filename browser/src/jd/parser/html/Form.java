//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.parser.html;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.http.requests.RequestVariable;
import jd.parser.Regex;
import jd.utils.EditDistance;

import org.appwork.utils.logging.Log;

public class Form {

    public enum MethodType {
        GET, POST, PUT, UNKNOWN
    }

    /**
     * 
     */
    private static final long serialVersionUID = 5837247484638868257L;

    /**
     * Ein Array mit allen Forms dessen Inhalt dem matcher entspricht. Achtung
     * der Matcher bezieht sich nicht auf die Properties einer Form sondern auf
     * den Text der zwischen der Form steht. DafÃ¼r gibt es die formProperties
     */
    public static Form[] getForms(final Object requestInfo) {
        final LinkedList<Form> forms = new LinkedList<Form>();

        final Pattern pattern = Pattern.compile("<[\\s]*form(.*?)>(.*?)<[\\s]*/[\\s]*form[\\s]*>|<[\\s]*form(.*?)>(.+)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        final Matcher formmatcher = pattern.matcher(requestInfo.toString().replaceAll("(?s)<!--.*?-->", ""));
        while (formmatcher.find()) {
            final String total = formmatcher.group(0);
            // System.out.println(inForm);

            final Form form = new Form(total);

            forms.add(form);

        }
        return forms.toArray(new Form[forms.size()]);
    }

    /**
     * Action der Form entspricht auch oft einer URL
     */
    private String action;

    private ArrayList<InputField> inputfields;

    private String htmlcode = null;

    private MethodType method = MethodType.GET;

    /* default encoding for http forms */
    private String encoding = "application/x-www-form-urlencoded";

    private InputField preferredSubmit;

    private final HashMap<String, String> keyValueMap;

    public Form() {
        this.inputfields = new ArrayList<InputField>();
        this.keyValueMap = new HashMap<String, String>();
    }

    public Form(final String total) {
        this();
        this.parse(total);
    }

    public void addInputField(final InputField nv) {

        this.inputfields.add(nv);

    }

    public void addInputFieldAt(final InputField nv, final int i) {

        this.inputfields.add(i, nv);

    }

    /**
     * Gibt zurÃ¼ck ob der gesuchte needle String im html Text bgefunden wurde
     * 
     * @param fileNotFound
     * @return
     */
    public boolean containsHTML(final String needle) {
        return new Regex(this.htmlcode, needle).matches();
    }

    public boolean equalsIgnoreCase(final Form f) {
        return this.toString().equalsIgnoreCase(f.toString());
    }

    public String getAction() {
        return this.action;
    }

    public String getAction(final String baseURL) {
        URL baseurl = null;
        if (baseURL == null) {
            baseurl = null;
        } else {
            try {
                baseurl = new URL(baseURL);
            } catch (final MalformedURLException e) {
                Log.exception(e);
            }
        }
        String ret = this.action;
        if (this.action == null || this.action.matches("[\\s]*")) {
            if (baseurl == null) { return null; }
            ret = baseurl.toString();
        } else if (!ret.matches("https?://.*")) {
            if (baseurl == null) { return null; }
            if (ret.charAt(0) == '/') {
                if (baseurl.getPort() > 0 && baseurl.getPort() != 80) {
                    ret = "http://" + baseurl.getHost() + ":" + baseurl.getPort() + ret;
                } else {
                    ret = "http://" + baseurl.getHost() + ret;
                }
            } else if (ret.charAt(0) == '&') {
                final String base = baseurl.toString();
                if (base.matches("http://.*/.*")) {
                    ret = base + ret;
                } else {
                    ret = base + "/" + ret;
                }
            } else if (ret.charAt(0) == '?') {
                final String base = baseurl.toString();
                if (base.matches("http://.*/.*")) {
                    ret = base.replaceFirst("\\?.*", "") + ret;
                } else {
                    ret = base + "/" + ret;
                }
            } else if (ret.charAt(0) == '#') {
                final String base = baseurl.toString();
                if (base.matches("http://.*/.*")) {
                    ret = base + ret;
                } else {
                    ret = base + "/" + ret;
                }
            } else {
                final String base = baseurl.toString();
                if (base.matches("http://.*/.*")) {
                    ret = base.substring(0, base.lastIndexOf("/")) + "/" + ret;
                } else {
                    ret = base + "/" + ret;
                }
            }
        }
        return ret;
    }

    /**
     * Gibt den variablennamen der am besten zu varname passt zurÃ¼ck.
     * 
     * @param varname
     * @return
     */
    public String getBestVariable(final String varname) {
        String best = null;
        int bestDist = Integer.MAX_VALUE;

        for (final InputField ipf : this.inputfields) {
            final int dist = EditDistance.getLevenshteinDistance(varname, ipf.getKey());
            if (dist < bestDist) {
                best = ipf.getKey();
                bestDist = dist;
            }
        }
        return best;

    }

    public String getEncoding() {
        return this.encoding;
    }

    public String getHtmlCode() {
        return this.htmlcode;
    }

    /**
     * Gets the first inputfiled with this key. REMEMBER. There can be more than
     * one file with this key
     * 
     * @param key
     * @return
     */
    public InputField getInputField(final String key) {
        for (final InputField ipf : this.inputfields) {
            if (ipf.getKey() != null && ipf.getKey().equalsIgnoreCase(key)) { return ipf; }
        }
        return null;
    }

    // public boolean hasSubmitValue(String value) {
    // for (String submit : this.submitValues) {
    // try {
    // if (submit == value || submit.equalsIgnoreCase(value)) return true;
    // } catch (NullPointerException e) {
    // //
    // jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE,
    // "Exception occurred",e);
    // }
    // }
    // return false;
    //
    // }

    public InputField getInputFieldByName(final String name) {
        for (final InputField ipf : this.inputfields) {
            if (ipf.getKey() != null && ipf.getKey().equalsIgnoreCase(name)) { return ipf; }
        }
        return null;

    }

    public InputField getInputFieldByProperty(final String key) {
        for (final InputField ipf : this.inputfields) {
            if (ipf.get(key) != null && ipf.get(key).equalsIgnoreCase(key)) { return ipf; }
        }
        return null;

    }

    public InputField getInputFieldByType(final String type) {
        for (final InputField ipf : this.inputfields) {
            if (ipf.getType() != null && ipf.getType().equalsIgnoreCase(type)) { return ipf; }
        }
        return null;

    }

    public ArrayList<InputField> getInputFields() {
        return this.inputfields;
    }

    public ArrayList<InputField> getInputFieldsByType(final String type) {
        final ArrayList<InputField> ret = new ArrayList<InputField>();
        for (final InputField ipf : this.inputfields) {
            if (ipf.getType() != null && org.appwork.utils.Regex.matches(ipf.getType(), type)) {
                ret.add(ipf);
            }
        }
        return ret;
    }

    public MethodType getMethod() {
        return this.method;
    }

    public InputField getPreferredSubmit() {

        return this.preferredSubmit;
    }

    /**
     * GIbt alle variablen als propertyString zurÃ¼ck
     * 
     * @return
     */
    public String getPropertyString() {
        final StringBuilder stbuffer = new StringBuilder();
        boolean first = true;
        for (final InputField ipf : this.inputfields) {
            /* nameless key-value are not being sent, see firefox */
            if (ipf.getKey() == null) {
                continue;
            }
            if (first) {
                first = false;
            } else {
                stbuffer.append("&");
            }
            stbuffer.append(ipf.getKey());
            stbuffer.append("=");
            stbuffer.append(ipf.getValue());
        }
        return stbuffer.toString();

    }

    /**
     * Gibt ein RegexObject bezÃ¼glich des Form htmltextes zurÃ¼ck
     * 
     * @param compile
     * @return
     */
    public Regex getRegex(final Pattern compile) {
        return new Regex(this.htmlcode, compile);
    }

    /**
     * Gibt ein RegexObject bezÃ¼glich des Form htmltextes zurÃ¼ck
     * 
     * @param compile
     * @return
     */
    public Regex getRegex(final String string) {
        return new Regex(this.htmlcode, string);
    }

    /**
     * Returns a list of requestvariables
     * 
     * @return
     */
    public ArrayList<RequestVariable> getRequestVariables() {
        final ArrayList<RequestVariable> ret = new ArrayList<RequestVariable>();
        for (final InputField ipf : this.inputfields) {
            // Do not send not prefered Submit types
            if (this.getPreferredSubmit() != null && ipf.getType() != null && ipf.getType().equalsIgnoreCase("submit") && this.getPreferredSubmit() != ipf) {
                continue;
            }
            if (ipf.getKey() == null) {
                continue;/*
                          * nameless key-value are not being sent, see firefox
                          */
            }
            if (ipf.getValue() == null) {
                continue;
            }
            if (ipf.getType() != null && ipf.getType().equalsIgnoreCase("image")) {
                ret.add(new RequestVariable(ipf.getKey() + ".x", new Random().nextInt(100) + ""));
                ret.add(new RequestVariable(ipf.getKey() + ".y", new Random().nextInt(100) + ""));
            } else {
                ret.add(new RequestVariable(ipf.getKey(), ipf.getValue()));
            }

        }
        return ret;
    }

    public String getStringProperty(final String property) {
        // TODO Auto-generated method stub
        return this.keyValueMap.get(property);
    }

    public HashMap<String, String> getVarsMap() {
        final HashMap<String, String> ret = new HashMap<String, String>();
        for (final InputField ipf : this.inputfields) {
            /* nameless key-value are not being sent, see firefox */
            if (ipf.getKey() == null) {
                continue;
            }
            ret.put(ipf.getKey(), ipf.getValue());
        }
        return ret;
    }

    public boolean hasInputFieldByName(final String name) {
        return this.getInputFieldByName(name) != null;
    }

    private void parse(final String total) {
        this.htmlcode = total;

        // form.baseRequest = requestInfo;
        final String header = new Regex(total, "<[\\s]*form(.*?)>").getMatch(0);
        //
        // <[\\s]*form(.*?)>(.*?)<[\\s]*/[\\s]*form[\\s]*>|<[\\s]*form(.*?)>(.+)
        final String[][] headerEntries = new Regex(header, "[\"' ](\\w+?)[ ]*=[ ]*[\"'](.*?)[\"']").getMatches();
        final String[][] headerEntries2 = new Regex(header, "[\"' ](\\w+?)[ ]*=[ ]*([^>^ ^\"^']+)").getMatches();

        this.parseHeader(headerEntries);
        this.parseHeader(headerEntries2);

        this.parseInputFields();

        // if (form.action == null) {
        // form.action =
        // requestInfo.getConnection().getURL().toString();
        // }
        // form.vars.add(form.getInputFields(inForm));

    }

    private void parseHeader(final String[][] headerEntries) {
        String key;
        String value;
        String lowvalue;
        for (final String[] entry : headerEntries) {
            key = entry[0];
            value = entry[1];
            lowvalue = value.toLowerCase();
            if (key.equalsIgnoreCase("action")) {
                this.setAction(value);
            } else if (key.equalsIgnoreCase("enctype")) {
                this.setEncoding(value);

            } else if (key.equalsIgnoreCase("method")) {

                if (lowvalue.matches(".*post.*")) {
                    this.setMethod(MethodType.POST);
                } else if (lowvalue.matches(".*get.*")) {
                    this.setMethod(MethodType.GET);
                } else if (lowvalue.matches(".*put.*")) {
                    this.setMethod(MethodType.PUT);
                } else {
                    this.setMethod(MethodType.POST);
                }
            } else {
                this.setProperty(key, value);
            }
        }

    }

    private void parseInputFields() {
        this.inputfields = new ArrayList<InputField>();
        final Matcher matcher = Pattern.compile("(?s)(<[\\s]*(input|textarea|select).*?>)", Pattern.CASE_INSENSITIVE).matcher(this.htmlcode);
        while (matcher.find()) {
            final InputField nv = InputField.parse(matcher.group(1));
            if (nv != null) {
                this.addInputField(nv);

            }
        }

    }

    /**
     * Changes the value of the first filed with the key key to value. if no
     * field exists, a new one is created.
     * 
     * @param key
     * @param value
     */

    public void put(final String key, final String value) {
        final InputField ipf = this.getInputField(key);
        if (ipf != null) {
            ipf.setValue(value);
        } else {
            this.inputfields.add(new InputField(key, value));
        }
    }

    /**
     * Removes the first inputfiled with this key. REMEMBER. There can be more
     * than one file with this key
     * 
     * @param key
     * @return
     */
    public void remove(final String key) {
        /*
         * inputfields extends hashmap which overrides hashCode, thats why we
         * use iterator here
         */
        final Iterator<InputField> it = this.inputfields.iterator();
        while (it.hasNext()) {
            final InputField ipf = it.next();
            if (ipf.getKey() == null && key == null) {
                it.remove();
                return;
            }
            if (ipf.getKey() != null && ipf.getKey().equalsIgnoreCase(key)) {
                it.remove();
                return;
            }
        }
    }

    public void setAction(final String action) {
        this.action = action;
    }

    public void setEncoding(final String encoding) {
        this.encoding = encoding;
    }

    public void setMethod(final MethodType method) {
        this.method = method;

    }

    /**
     * Us the i-th submit field when submitted
     * 
     * @param i
     */
    public void setPreferredSubmit(int i) {
        this.preferredSubmit = null;
        for (final InputField ipf : this.inputfields) {
            if (ipf.getType() != null && ipf.getValue() != null && ipf.getType().equalsIgnoreCase("submit") && i-- <= 0) {
                this.preferredSubmit = ipf;
                return;
            }
        }

        throw new IllegalArgumentException("No such Submitfield: " + i);

    }

    /**
     * Tell the form which submit field to use
     * 
     * @param preferredSubmit
     */
    public void setPreferredSubmit(final String preferredSubmit) {
        this.preferredSubmit = null;
        for (final InputField ipf : this.inputfields) {
            if (ipf.getType() != null && ipf.getValue() != null && ipf.getType().equalsIgnoreCase("submit") && ipf.getValue().equalsIgnoreCase(preferredSubmit)) {
                this.preferredSubmit = ipf;
                return;
            }
        }
        Log.L.warning("No exact match for submit found! Trying to find best match now!");
        for (final InputField ipf : this.inputfields) {
            if (ipf.getType() != null && ipf.getValue() != null && ipf.getType().equalsIgnoreCase("submit") && ipf.getValue().contains(preferredSubmit)) {
                this.preferredSubmit = ipf;
                return;
            }
        }
        throw new IllegalArgumentException("No such Submitfield: " + preferredSubmit);

    }

    public void setProperty(final String key, final String value) {
        this.keyValueMap.put(key, value);

    }

    @Override
    public String toString() {
        final StringBuilder ret = new StringBuilder();
        ret.append("Action: ");
        ret.append(this.action);
        ret.append('\n');
        if (this.method == MethodType.POST) {
            ret.append("Method: POST\n");
        } else if (this.method == MethodType.GET) {
            ret.append("Method: GET\n");
        } else if (this.method == MethodType.PUT) {
            ret.append("Method: PUT is not supported\n");

        } else if (this.method == MethodType.UNKNOWN) {
            ret.append("Method: Unknown\n");
        }
        for (final InputField ipf : this.inputfields) {

            ret.append(ipf.toString());
            ret.append('\n');
        }

        ret.append(this.keyValueMap.toString());
        return ret.toString();
    }

}
