/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.launch;

/**
 * Created by sd on 28.11.16.
 */
public class LaunchVPNException extends Exception {
    public final int vpnok;
    public LaunchVPNException(int vpnok){
        this.vpnok = vpnok;
    }
}
