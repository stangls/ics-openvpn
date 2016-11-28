/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.launch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;

import de.blinkt.openvpn.LaunchVPN;
import de.blinkt.openvpn.R;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ProfileManager;
import de.blinkt.openvpn.core.VPNLaunchHelper;
import de.blinkt.openvpn.core.VpnStatus;

import static android.content.ContentValues.TAG;

public class LaunchReceiver extends BroadcastReceiver {

    public LaunchReceiver() {}

    @Override
    public void onReceive(Context context, Intent intent) {
        VpnProfile profile = connect(context, intent);
        try {
            if (launchVPN( context, profile )!=null) {
                Log.e(TAG, "Launch-VPN requires user-interaction :-/" );
                restartLaunchInActivity(context,intent);
                return;
            }
        } catch (LaunchVPNException e) {
            e.printStackTrace();
        }
        int needpw = profile.needUserPWInput(false);
        if (needpw != 0) {
            Log.e(TAG, "Launch-VPN requires user-interaction (password)" );
            restartLaunchInActivity(context,intent);
            return;
        }
        VPNLaunchHelper.startOpenVpn(profile, context.getApplicationContext());
    }

    private void restartLaunchInActivity(Context context, Intent intent) {
        Intent aIntent = new Intent(intent);
        aIntent.setAction("android.intent.action.MAIN");
        aIntent.addCategory("android.intent.category.DEFAULT");
        context.startActivity(aIntent);
    }

    public static VpnProfile connect(Context context, Intent intent) {
        String shortcutUUID = intent.getStringExtra(LaunchVPN.EXTRA_KEY);
        String shortcutName = intent.getStringExtra(LaunchVPN.EXTRA_NAME);

        VpnProfile profileToConnect = ProfileManager.get(context, shortcutUUID);
        if (shortcutName != null && profileToConnect == null)
            profileToConnect = ProfileManager.getInstance(context).getProfileByName(shortcutName);
        return profileToConnect;
    }

    public static Intent launchVPN(Context context, VpnProfile profile) throws LaunchVPNException {
        int vpnok = profile.checkProfile(context);
        if (vpnok != R.string.no_error_found) {
            throw new LaunchVPNException(vpnok);
        }

        Intent intent = VpnService.prepare(context);
        // Check if we want to fix /dev/tun
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean usecm9fix = prefs.getBoolean("useCM9Fix", false);
        boolean loadTunModule = prefs.getBoolean("loadTunModule", false);

        boolean mCmfixed = false;
        if (loadTunModule)
            mCmfixed = execeuteSUcmd("insmod /system/lib/modules/tun.ko");

        if (usecm9fix && !mCmfixed) {
            execeuteSUcmd("chown system /dev/tun");
        }

        return intent;
    }

    private static boolean execeuteSUcmd(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder("su", "-c", command);
            Process p = pb.start();
            int ret = p.waitFor();
            return (ret == 0);
        } catch (InterruptedException | IOException e) {
            VpnStatus.logException("SU command", e);
            return false;
        }
    }
}
