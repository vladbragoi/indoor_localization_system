package it.univr.vlad.fingerprinting.model;


public abstract class CBLAbstract { /*implements Replication.ChangeListener{
    private AndroidContext context;
    private Manager manager;
    private com.couchbase.lite.Database database;

    private Replication pushReplication;
    private Replication pullReplication;

    private String dbName;
    private String dbUrl;
    private static boolean shown = false;*/
/*
    CouchbaseAbstract(Context context, String dbName, String dbUrl) {
        this.context = new AndroidContext(context);
        this.manager = getManager();
        this.dbName = dbName;
        this.dbUrl = dbUrl;
        try {
            this.database = manager.getDatabase(dbName);
        } catch (CouchbaseLiteException e) {
            Log.e(dbName, "Cannot get " + dbName, e);
        }
        if (dbUrl != null)
            updateUrl(dbUrl);

        database.addChangeListener(new Database.ChangeListener() {
            @Override
            public void changed(Database.ChangeEvent event) {
                Log.v(dbName, event.getChanges().toString());
            }
        });
        // CouchbaseLiteHttpClientFactory clientFactory = new CouchbaseLiteHttpClientFactory(database.getPersistentCookieStore());
        // clientFactory.allowSelfSignedSSLCertificates();
        // manager.setDefaultHttpClientFactory(clientFactory);
    }

    public abstract void start();

    public abstract void stop();

    void startPushReplication(boolean continuous, String filter) {
        if (pushReplication == null) {
            try {
                pushReplication = database.createPushReplication(getSyncUrl());
            } catch (MalformedURLException e) {
                Log.e(database.getName(), "PUSH: url error");
                return;
            } catch (UrlDocumentNotFoundException e) {
                Log.w(database.getName(), "Cannot fetch url document");
                return;
            }
            pushReplication.setAuthenticator(getAuth());
            pushReplication.setContinuous(continuous);
            if (filter != null)
                pushReplication.setFilter(filter);
        }
        pushReplication.addChangeListener(this);
        if (!database.isOpen()) {
            try {
                database.open();
            } catch (CouchbaseLiteException e) {
                Log.w(dbName, "Cannot open database");
            }
        }
        pushReplication.start();
    }

    void stopPushReplication() {
        if (pushReplication != null) {
            pushReplication.stop();
            pushReplication.removeChangeListener(this);

            if (database != null)
                database.close();

            if (manager != null)
                manager.close();
        }
    }

    protected void startPullReplication(boolean continuous, String filter) {
        if (pullReplication == null) {
            try {
                pullReplication = database.createPullReplication(getSyncUrl());
            } catch (MalformedURLException e) {
                Log.e(database.getName(), "PULL: url error");
                return;
            } catch (UrlDocumentNotFoundException e) {
                Log.w(database.getName(), "Cannot fetch url document");
                return;
            }
            pullReplication.setAuthenticator(getAuth());
            pullReplication.setContinuous(continuous);
            if (filter != null)
                pullReplication.setFilter(filter);
        }
        pullReplication.addChangeListener(this);
        if (!database.isOpen()) {
            try {
                database.open();
            } catch (CouchbaseLiteException e) {
                Log.w(dbName, "Cannot open database");
            }
        }
        pullReplication.start();
    }

    void stopPullReplication() {
        if (pullReplication != null) {
            pullReplication.stop();
            pullReplication.removeChangeListener(this);

            if (database != null)
                database.close();

            if (manager != null)
                manager.close();
        }
    }

    @Override
    public void changed(Replication.ChangeEvent event) {
        if (event.getError() != null) {
            Throwable lastError = event.getError();
            if (lastError instanceof UnknownHostException) stop();
            if (lastError instanceof RemoteRequestResponseException) {
                RemoteRequestResponseException exception = (RemoteRequestResponseException) lastError;
                switch (exception.getCode()) {
                    case 401:
                        Log.e(database.getName(), "Authentication failed");
                        break;
                    case 400:
                        Log.e(database.getName(), "Bad request");
                        break;
                    case 404:
                        Log.e(database.getName(), "Not found");
                        break;
                    default:
                        Log.e(database.getName(), "Code error: " + exception.getCode());
                }
            }
        }
    }

    private Manager getManager() {
        if (manager == null) {
            try {
                manager = new Manager(context, Manager.DEFAULT_OPTIONS);
            } catch (Exception e) {
                Log.e("MANAGER", "Cannot create Manager object", e);
            }
        }



        // Manager.enableLogging(Log.TAG, Log.WARN);

        // Manager.enableLogging(Log.TAG, Log.VERBOSE);
        // Manager.enableLogging(Log.TAG_ROUTER, Log.VERBOSE);
        // Manager.enableLogging(Log.TAG_VIEW, Log.VERBOSE);
        // Manager.enableLogging(Log.TAG_QUERY, Log.VERBOSE);
        // Manager.enableLogging(Log.TAG_MULTI_STREAM_WRITER, Log.VERBOSE);
        // Manager.enableLogging(Log.TAG_LISTENER, Log.VERBOSE);
        // Manager.enableLogging(Log.TAG_BLOB_STORE, Log.VERBOSE);
        // Manager.enableLogging(Log.TAG_SYNC_ASYNC_TASK, Log.VERBOSE);
        // Manager.enableLogging(Log.TAG_SYNC, Log.VERBOSE);
        // Manager.enableLogging(Log.TAG_BATCHER, Log.VERBOSE);
        // Manager.enableLogging(Log.TAG_CHANGE_TRACKER, Log.VERBOSE);
        // Manager.enableLogging(Log.TAG_REMOTE_REQUEST, Log.VERBOSE);
        // Manager.enableLogging(Log.TAG_DATABASE, Log.VERBOSE);

        return manager;
    }

    private URL getSyncUrl() throws MalformedURLException, UrlDocumentNotFoundException {
        if (this.dbUrl == null) {
            Object url = database.getDocument("url").getProperty("value");
            if (url instanceof String) {
                DataHandler.INSTANCE.setUrl(url.toString());
                return new URL(url.toString() + dbName);
            } else {
                if (!shown) {
                    showUrlDialog();
                    shown = true;
                }
                throw new UrlDocumentNotFoundException();
            }
        }
        else return new URL(this.dbUrl + dbName);
    }

    private void showUrlDialog() {
        DialogFragment dialog = new UrlDialogFragment();
        dialog.setCancelable(false);
        try {
            MainActivity activity = (MainActivity) context.getWrappedContext();
            dialog.show(activity.getSupportFragmentManager(), "url");
        }
        catch (ClassCastException e) {
            Log.d("UrlDialog", "Cannot get fragment manager");
        }
    }

    private void updateUrl(String url) {
        DataHandler.INSTANCE.setUrl(url);
        Map<String, Object> property = new HashMap<>();
        property.put("value", url);
        Document doc = database.getDocument("url");
        UnsavedRevision rev = doc.createRevision();
        rev.setProperties(property);
        try {
            rev.save();
            Log.i(dbName, "Document updated. URL: " + url);
        } catch (CouchbaseLiteException e) {
            Log.w(dbName, "Cannot update url");
        }
    }

    public Document getDocument(String docName) {
        return database.getDocument(docName);
    }

    private Authenticator getAuth() {
        return AuthenticatorFactory.createBasicAuthenticator("admin",
                "admin");
    }

    @Override
    public String toString() {
        return this.dbName;
    }

    public static class UrlDialogFragment extends DialogFragment {

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final LayoutInflater inflater = getActivity().getLayoutInflater();
            final View urlView = inflater.inflate(R.layout.dialog_url, null);

            // Inflate and set the layout for the dialog and add action buttons
            final AlertDialog alertDialog = new AlertDialog.Builder(getContext())
                    .setView(urlView)
                    .setTitle("URL CouchDB")
                    .setPositiveButton("Ok", null)
                    .setNegativeButton("Annulla", null)
                    .create();

            alertDialog.setOnShowListener(
                    (final DialogInterface dialogInterface) -> { // lambda expression
                        Button positive = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);

                        positive.setOnClickListener(
                                (View view) -> { // lambda expression
                                    EditText urlText = urlView.findViewById(R.id.url);
                                    String url = urlText.getText().toString().trim();
                                    if (URLUtil.isValidUrl(url)) {
                                        DataHandler.INSTANCE.getDatabase().updateUrl(url);
                                        dialogInterface.dismiss();
                                    } else urlText.setError("URL non valido! ");
                                });
                    });

            return alertDialog;
        }
    }*/
}
