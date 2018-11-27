/**
 * Wi-Fi в метро (pw.thedrhax.mosmetro, Moscow Wi-Fi autologin)
 * Copyright © 2015 Dmitry Karikh <the.dr.hax@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pw.thedrhax.mosmetro.authenticator.providers;

import android.content.Context;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.authenticator.InitialConnectionCheckTask;
import pw.thedrhax.mosmetro.authenticator.NamedTask;
import pw.thedrhax.mosmetro.authenticator.Provider;
import pw.thedrhax.mosmetro.httpclient.ParsedResponse;
import pw.thedrhax.util.Logger;

/**
 * The MAInet class implements support for the public Wi-Fi network of MAI (MAInet_public).
 *
 * Detection: Meta or Location redirect contains "wi-fi.mai.ru".
 *
 * @author Dmitry Karikh <the.dr.hax@gmail.com>
 * @see Provider
 */

public class MAInet extends Provider {
    private String redirect = "https://wi-fi.mai.ru/login.html";

    public MAInet(Context context, ParsedResponse res) {
        super(context);

        /**
         * Checking Internet connection
         * ⇒ GET generate_204 < res
         * ⇐ Meta or Location redirect: https://wi-fi.mai.ru/login.html?... > redirect
         */
        add(new InitialConnectionCheckTask(this, res) {
            @Override
            public boolean handle_response(HashMap<String, Object> vars, ParsedResponse response) {
                try {
                    redirect = response.parseAnyRedirect();
                    Logger.log(Logger.LEVEL.DEBUG, redirect);
                } catch (ParseException ex) {
                    Logger.log(Logger.LEVEL.DEBUG, ex);
                    Logger.log(Logger.LEVEL.DEBUG, "Redirect not found in response, using default");
                }

                return true;
            }
        });

        /**
         * Sending auth form
         * ⇒ POST https://wifi.mai.ru/login.html < redirect
         * ⇐ ???
         */
        add(new NamedTask(context.getString(R.string.auth_auth_form)) {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                try {
                    ParsedResponse response = client.post(redirect, new HashMap<String, String>() {{
                        put("buttonClicked", "4");
                        put("err_flag", "0");
                        put("err_msg", "");
                        put("info_flag", "0");
                        put("info_msg", "");
                        put("redirect_url", "http://google.com/generate_204");
                        put("network_name", "Guest+Network");
                        put("username", "MAI");
                        put("password", "1930");
                    }}, pref_retry_count);

                    Logger.log(Logger.LEVEL.DEBUG, response.toString());
                } catch (IOException ex) {
                    Logger.log(Logger.LEVEL.DEBUG, ex);
                    Logger.log(context.getString(R.string.error,
                            context.getString(R.string.auth_error_server)
                    ));
                    return false;
                }

                return true;
            }
        });

        /**
         * Checking Internet connection
         */
        add(new NamedTask(context.getString(R.string.auth_checking_connection)) {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                if (isConnected()) {
                    Logger.log(context.getString(R.string.auth_connected));
                    vars.put("result", RESULT.CONNECTED);
                    return true;
                } else {
                    Logger.log(context.getString(R.string.error,
                            context.getString(R.string.auth_error_connection)
                    ));
                    return false;
                }
            }
        });
    }

    /**
     * Checks if current network is supported by this Provider implementation.
     * @param response  Instance of ParsedResponse.
     * @return          True if response matches this Provider implementation.
     */
    public static boolean match(ParsedResponse response) {
        try {
            return response.parseAnyRedirect().contains("wi-fi.mai.ru");
        } catch (ParseException ex1) {
            return false;
        }
    }
}
