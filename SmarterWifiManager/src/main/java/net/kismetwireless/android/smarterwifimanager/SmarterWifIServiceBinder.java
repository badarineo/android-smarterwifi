package net.kismetwireless.android.smarterwifimanager;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import java.util.ArrayList;

class SmarterWifiServiceBinder {
    private SmarterWifiService smarterService;
    private boolean isBound;
    Context context;
    BinderCallback onBindCb;

    ArrayList<SmarterWifiService.SmarterServiceCallback> pendingList = new ArrayList<SmarterWifiService.SmarterServiceCallback>();
    ArrayList<SmarterWifiService.SmarterServiceCallback> registeredList = new ArrayList<SmarterWifiService.SmarterServiceCallback>();

    public static class BinderCallback {
        public void run(SmarterWifiService service) {
            return;
        }
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            SmarterWifiService.ServiceBinder binder = (SmarterWifiService.ServiceBinder) service;
            smarterService = binder.getService();

            isBound = true;

            synchronized (this) {
                if (pendingList.size() > 0) {
                    for (SmarterWifiService.SmarterServiceCallback cb : pendingList) {
                        smarterService.addCallback(cb);
                        registeredList.add(cb);
                    }
                }

                pendingList.clear();
            }

            if (onBindCb != null)
                onBindCb.run(smarterService);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isBound = false;
        }
    };

    public SmarterWifiServiceBinder(Context c) {
        context = c;
    }

    public boolean getIsBound() {
        return isBound;
    }

    public SmarterWifiService getService() {
        return smarterService;
    }

    void doKillService() {
        if (smarterService == null)
            return;

        if (!isBound)
            return;

        smarterService.shutdownService();
    }

    // Call a cb as soon as we finish binding
    void doCallAndBindService(BinderCallback cb) {
        if (isBound)
            cb.run(smarterService);

        onBindCb = cb;

        doBindService();
    }

    void doBindService() {
        if (isBound)
            return;

        // Might as well always try to start
        Intent svc = new Intent(context.getApplicationContext(), SmarterWifiService.class);
        context.getApplicationContext().startService(svc);

        // We want to bind in the application context
        context.getApplicationContext().bindService(new Intent(context, SmarterWifiService.class),
                serviceConnection, Context.BIND_AUTO_CREATE);
    }


    public void doUnbindService() {
        if (isBound) {
            if (smarterService != null) {
                for (SmarterWifiService.SmarterServiceCallback cb : registeredList)
                        smarterService.removeCallback(cb);

                // If we can't unbind just silently ignore it
                try {
                    context.unbindService(serviceConnection);
                } catch (IllegalArgumentException e) {

                }
            }
        }

        smarterService = null;
        isBound = false;
    }

    public void doUpdatePreferences() {
        if (smarterService != null)
            smarterService.updatePreferences();
    }

    public void addCallback(SmarterWifiService.SmarterServiceCallback cb) {
        synchronized (this) {
            if (smarterService == null) {
                pendingList.add(cb);
            } else {
                smarterService.addCallback(cb);
            }
        }
    }

    public void removeCallback(SmarterWifiService.SmarterServiceCallback cb) {
        synchronized (this) {
            if (smarterService == null) {
                pendingList.remove(cb);
            } else {
                smarterService.removeCallback(cb);
                registeredList.remove(cb);
            }
        }
    }
}