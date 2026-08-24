package com.porfirio.orariprocida2011.dialogs;


import static android.view.Gravity.END;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.porfirio.orariprocida2011.R;
import com.porfirio.orariprocida2011.activities.OrariProcida2011Activity;
import com.porfirio.orariprocida2011.entity.Alert;
import com.porfirio.orariprocida2011.entity.Compagnia;
import com.porfirio.orariprocida2011.entity.Meteo;
import com.porfirio.orariprocida2011.entity.Mezzo;
import com.porfirio.orariprocida2011.entity.Taxi;
import com.porfirio.orariprocida2011.threads.alerts.AlertsDAO;
import com.porfirio.orariprocida2011.threads.taxies.TaxisDAO;
import com.porfirio.orariprocida2011.utils.Analytics;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class DettagliMezzoDialog extends DialogFragment implements OnClickListener {
    TextView txtPartenzaDestinazione;
    Button btnTaxi;
    Button buttonSegnala;
    Button btnBiglietterie;
    Button buttonConferma;
    private Compagnia c;
    private LinearLayout linear_layout_dettagli_mezzo;
    private Mezzo mezzo;
    private Context callingContext;
    private Calendar calen;
    private OrariProcida2011Activity callingActivity;
    private ArrayList<Compagnia> lc;

    private final AlertsDAO alertsDAO;
    private final TaxisDAO taxisDAO;
    private Analytics analytics;
    private View currentDynamicView = null;
    private String porto;
    private List<Taxi> taxis;
    private View view_separator;
    private int ragione;
    private boolean reportShortcut = false;
    private OnReportListener onReportListener;
    private Meteo meteo;

    public interface OnReportListener {
        void onDialogDismissed();
    }

    public void setOnReportListener(OnReportListener listener) {
        this.onReportListener = listener;
    }

    public DettagliMezzoDialog(AlertsDAO alertsDAO, TaxisDAO taxisDAO) {
        this.alertsDAO = Objects.requireNonNull(alertsDAO);
        this.taxisDAO = taxisDAO;
    }

    public void setDettagliMezzoDialog(FragmentManager fm, OrariProcida2011Activity a, Context context, Calendar cal, Meteo meteo) {
        callingActivity = a;
        callingContext = context;
        calen = cal;
        this.meteo = meteo;
    }

    public void setAnalytics(Analytics analytics) {
        this.analytics = analytics;
    }

    public void setReportShortcut(boolean reportShortcut) {
        this.reportShortcut = reportShortcut;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            //this makes dialog's width 90% on phone and 50% on tablet
            boolean isTablet = getResources().getConfiguration().smallestScreenWidthDp >= 600;
            boolean isSplitScreen = requireActivity().isInMultiWindowMode();

            float widthFactor = (isTablet && !isSplitScreen) ? 0.5f : 0.9f;
            int width = (int) (getResources().getDisplayMetrics().widthPixels * widthFactor);

            getDialog().getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {


        View view = inflater.inflate(R.layout.dettaglimezzo, container);
        linear_layout_dettagli_mezzo = view.findViewById(R.id.linear_layout_dettagli_mezzo);

        txtPartenzaDestinazione = view.findViewById(R.id.txtPartenzaDestinazione);
        TextView txtMezzo = view.findViewById(R.id.txtMezzo);
        TextView txtPartenza = view.findViewById(R.id.txtPartenza);
        TextView txtArrivo = view.findViewById(R.id.txtArrivo);
        TextView txtCostoIntero = view.findViewById(R.id.txtCostoIntero);
        TextView txtCostoRidotto = view.findViewById(R.id.txtCostoRidotto);
        TextView txtAuto = view.findViewById(R.id.txtAuto);
        TextView txtAllertaMeteo = view.findViewById(R.id.txtAllertaMeteo);
        view_separator = view.findViewById(R.id.view_separator);


        btnTaxi = view.findViewById(R.id.btnTaxi);
        btnTaxi.setOnClickListener(v -> {
            analytics.send("App Event", "Click Taxi Dialog");
            toggleGrid(callingActivity.getString(R.string.numeriTaxi));
            updateButtonStates(callingActivity.getString(R.string.numeriTaxi));
        });

        btnBiglietterie = view.findViewById(R.id.btnBiglietterie);
        btnBiglietterie.setOnClickListener(v -> {
            analytics.send("App Event", "Click Biglietterie Dialog");
            toggleGrid(callingActivity.getString(R.string.numeriUtili));
            updateButtonStates(callingActivity.getString(R.string.numeriUtili));
        });

        buttonSegnala = view.findViewById(R.id.btnSegnala);
        buttonSegnala.setOnClickListener(v -> {
            if (!callingActivity.isOnline())
                Toast.makeText(getContext(), callingActivity.getString(R.string.soloOnline), Toast.LENGTH_SHORT).show();
            else {
                analytics.send("App Event", "Click Segnalazione Dialog");
                toggleReportGrid();
                updateButtonStates(callingActivity.getString(R.string.confermaOSmentisci));
            }
        });

        if (mezzo != null) {
            txtMezzo.setText(mezzo.nave);
        } else {
            Log.d("DettagliMezzoDialog", "Errore: oggetto Mezzo non esiste");
        }


        String s = mezzo.portoPartenza + " - " + mezzo.portoArrivo;
        txtPartenzaDestinazione.setText(s);

        s = mezzo.portoPartenza + " - " + DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(mezzo.getDepartureTime());
        txtPartenza.setText(s);

        s = mezzo.portoArrivo + " - " + DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(mezzo.getArrivalTime());
        txtArrivo.setText(s);


        if (mezzo.getReducedPrice() > 0) {
            String ridotto = String.format(Locale.getDefault(), "%.2f", mezzo.getReducedPrice()) + " € ";
            txtCostoRidotto.setText(ridotto);
        }


        if (mezzo.getFullPrice() > 0) {
            String intero = String.format(Locale.getDefault(), "%.2f", mezzo.getFullPrice()) + " € ";
            txtCostoIntero.setText(intero);
        }


        //trova compagnia c
        c = null;
        for (int i = 0; i < lc.size(); i++) {
            if (mezzo.nave.contains(lc.get(i).getName()))
                c = lc.get(i);
        }

        //Aggiunto Aladino
        if (c != null) {
            if (c.getName().contains("Ippocampo") || c.getName().contentEquals("Procida Lines") || mezzo.nave.contains("Aliscafo") || mezzo.nave.contains("Aladino") || mezzo.nave.contains("Motonave") || mezzo.nave.contains("Scotto Line"))
                txtAuto.setText(callingContext.getString(R.string.trasportaSoloPasseggeri));
            else
                txtAuto.setText(callingContext.getString(R.string.trasportaAutoPasseggeri));
        } else
            txtAuto.setText(R.string.nessunaInfoTrasportoVeicoli);
        this.porto = mezzo.portoPartenza;

        taxisDAO.getUpdates().observe(this, update -> {
            taxis = update.getData();
        });


        String[] ragioni = getResources().getStringArray(R.array.strRagioni);
        String spc = "";
        if (mezzo.segnalazionePiuComune() > -1) {
            spc = ragioni[mezzo.segnalazionePiuComune()];
        }
        if (mezzo.tot > 0 || mezzo.conferme > 0 || !getWeatherConditionsString(getContext(), mezzo, calen).isEmpty()) {
            StringBuilder alert = new StringBuilder();
            if (mezzo.tot > 0) {
                if (mezzo.conc) {
                    alert.append(mezzo.tot).append(mezzo.tot == 1 ? " " + getString(R.string.segnalazione) : " " + getString(R.string.segnalazioni));
                    alert.append(" ").append(getString(R.string.diProblemi)).append(" : ").append(spc);
                } else {
                    alert.append(getString(R.string.possibiliProblemi)).append(" (").append(mezzo.tot);
                    alert.append(mezzo.tot == 1 ? " " + getString(R.string.segnalazione) + ")" : " " + getString(R.string.segnalazioni) + ")");
                    alert.append(", ").append(getString(R.string.inParticolare)).append(" ").append(spc);
                }
            }
            if (mezzo.conferme > 0) {
                alert.append(mezzo.conferme).append(mezzo.conferme == 1 ? " " + getString(R.string.utenteDice) : " " + getString(R.string.utentiDicono));
                alert.append(" ").append(getString(R.string.cheLaCorsaERegolare));
            }
            if (!getWeatherConditionsString(getContext(), mezzo, calen).isEmpty()) {
                alert.append(getWeatherConditionsString(getContext(), mezzo, calen));
            }
            txtAllertaMeteo.setVisibility(VISIBLE);
            txtAllertaMeteo.setText(alert);
        }

        buttonConferma = view.findViewById(R.id.buttonConferma);
        buttonConferma.setOnClickListener(v -> {
            if (!callingActivity.isOnline()) {
                Toast.makeText(getContext(), callingActivity.getString(R.string.soloOnline), Toast.LENGTH_SHORT).show();
            } else {
                if (scriviSegnalazione(false, null)) {
                    Toast.makeText(getContext(), R.string.invioConfermaAvvenuto, Toast.LENGTH_SHORT).show();
                    dismiss();
                } else {
                    Toast.makeText(getContext(), R.string.invioConfermaFallito, Toast.LENGTH_SHORT).show();
                }
            }
        });

        if (reportShortcut) {
            buttonSegnala.callOnClick();
        }

        return view;
    }

    public void setMezzo(Mezzo m) {
        mezzo = m;
    }

    @Override
    public void onClick(View arg0) {
        this.dismiss();
    }

    public void setListCompagnia(ArrayList<Compagnia> listCompagnia) {
        lc = listCompagnia;
    }

    private void toggleGrid(String type) {
        if (currentDynamicView != null) {
            view_separator.setVisibility(GONE);
            linear_layout_dettagli_mezzo.removeView(currentDynamicView);
            if (currentDynamicView.getTag().equals(type)) {
                currentDynamicView = null;
                return;
            }
        }

        currentDynamicView = createGridLayout(type);
        view_separator.setVisibility(VISIBLE);
        linear_layout_dettagli_mezzo.addView(currentDynamicView);
    }

    //dynamically creates a grid layout for taxi or ticket
    private GridLayout createGridLayout(String type) {
        boolean isTablet = getResources().getConfiguration().smallestScreenWidthDp >= 600;
        boolean isSplitScreen = requireActivity().isInMultiWindowMode();

        int textsize;
        if (isTablet) {
            textsize = 28;
        } else if (isSplitScreen) {
            textsize = 24;
        } else {
            textsize = 20;
        }

        GridLayout gridLayout = new GridLayout(getContext());
        gridLayout.setTag(type);
        gridLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        gridLayout.setUseDefaultMargins(true);
        gridLayout.setColumnCount(2);
        gridLayout.setPadding(10, 10, 10, 10);

        TextView labelView = new TextView(getContext());
        labelView.setText(type);
        labelView.setTextColor(ContextCompat.getColor(requireContext(), R.color.button_dettagli_mezzo));
        labelView.setTextSize(textsize);
        labelView.setGravity(Gravity.CENTER);
        labelView.setPadding(0, 0, 0, 10);

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.rowSpec = GridLayout.spec(0);
        params.columnSpec = GridLayout.spec(0, 2);
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;

        labelView.setLayoutParams(params);

        gridLayout.addView(labelView);

        if (type.equals(callingActivity.getString(R.string.numeriTaxi))) {
            ArrayList<Taxi> taxiPortoList = new ArrayList<>();

            for (int i = 0; i < taxis.size(); i++)
                if (porto.contains(taxis.get(i).getPorto()) && !(porto.contentEquals("Monte di Procida") && taxis.get(i).getPorto().contentEquals("Procida")))
                    taxiPortoList.add(taxis.get(i));

            if (!taxiPortoList.isEmpty()) {
                for (Taxi taxi : taxiPortoList) {
                    addGridItem(gridLayout, taxi.getCompagnia(), taxi.getNumero(), true);
                }
            }
        } else if (type.equals(callingActivity.getString(R.string.numeriUtili))) {
            if (c == null) {
                addGridItem(gridLayout, getString(R.string.NoBiglietterie), "", false);
            } else {
                int contactsCount = c.getContactsCount();
                for (int i = 0; i < contactsCount; i++) {
                    String contactName = c.getContactName(i);
                    String contactNumber = c.getContactNumber(i);
                    addGridItem(gridLayout, contactName, contactNumber, true);
                }
            }
        }

        return gridLayout;
    }

    //dynamically creates a label:value item for taxi or ticket
    private void addGridItem(GridLayout grid, String label, String value, boolean addLinkify) {
        boolean isTablet = getResources().getConfiguration().smallestScreenWidthDp >= 600;
        boolean isSplitScreen = requireActivity().isInMultiWindowMode();

        int textsize;
        if (isTablet) {
            textsize = 20;
        } else if (isSplitScreen) {
            textsize = 18;
        } else {
            textsize = 16;
        }

        TextView labelView = new TextView(getContext());

        // If the label is "NoBiglietteria", create only the label without ":" and without a value
        if (label.equals(getString(R.string.NoBiglietterie))) {
            labelView.setText(label);
            labelView.setTypeface(null, Typeface.BOLD);
            labelView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey));
            labelView.setGravity(Gravity.START);
            labelView.setTextSize(textsize);

            GridLayout.LayoutParams labelParams = new GridLayout.LayoutParams();
            labelParams.width = GridLayout.LayoutParams.WRAP_CONTENT;
            labelParams.height = GridLayout.LayoutParams.WRAP_CONTENT;
            labelParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 2); // Occupies both columns
            labelView.setLayoutParams(labelParams);

            grid.addView(labelView);
            return;
        }

        labelView.setText(String.format("%s:", label));
        labelView.setTypeface(null, Typeface.BOLD);
        labelView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey));
        labelView.setGravity(Gravity.END);
        labelView.setTextSize(textsize);

        GridLayout.LayoutParams labelParams = new GridLayout.LayoutParams();
        labelParams.width = 0;
        labelParams.height = GridLayout.LayoutParams.WRAP_CONTENT;
        labelParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        labelView.setLayoutParams(labelParams);

        TextView valueView = new TextView(getContext());
        valueView.setText(value);
        valueView.setGravity(Gravity.START);
        valueView.setTextColor(ContextCompat.getColor(requireContext(), R.color.tertiaryColor));
        valueView.setTextSize(textsize);

        GridLayout.LayoutParams valueParams = new GridLayout.LayoutParams();
        valueParams.width = 0;
        valueParams.height = GridLayout.LayoutParams.WRAP_CONTENT;
        valueParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        valueView.setLayoutParams(valueParams);

        if (addLinkify) {
            Linkify.addLinks(valueView, Linkify.PHONE_NUMBERS);
        }

        grid.addView(labelView);
        grid.addView(valueView);
    }

    //dynamically shows or hides report layout and a separator line
    private void toggleReportGrid() {
        if (currentDynamicView != null) {
            view_separator.setVisibility(GONE);
            linear_layout_dettagli_mezzo.removeView(currentDynamicView);
            if (callingActivity.getString(R.string.confermaOSmentisci).equals(currentDynamicView.getTag())) {
                currentDynamicView = null;
                return;
            }
        }
        currentDynamicView = createReportLinearLayout();
        view_separator.setVisibility(VISIBLE);
        linear_layout_dettagli_mezzo.addView(currentDynamicView);
    }

    //dynamically creates a linear layout for report
    private LinearLayout createReportLinearLayout() {
        int marginHorizontal = 15, marginVertical = 8;
        float scale = requireContext().getResources().getDisplayMetrics().density;
        int marginHorizontalPx = (int) (marginHorizontal * scale + 0.5f);
        int marginVerticalPx = (int) (marginVertical * scale + 0.5f);

        LinearLayout linearLayout = new LinearLayout(getContext());
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(marginHorizontalPx, marginVerticalPx, marginHorizontalPx, marginVerticalPx);

        linearLayout.setLayoutParams(layoutParams);
        linearLayout.setPadding(10, 10, 10, 10);
        linearLayout.setTag(callingActivity.getString(R.string.confermaOSmentisci));


        Spinner spnRagioni = new Spinner(callingContext);
        spnRagioni.setPopupBackgroundResource(R.drawable.spinner_dropdown_background);
        spnRagioni.setBackgroundResource(R.drawable.dropdown_background_report_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                callingContext, R.array.strRagioni, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(R.layout.spinner_item);
        LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        spnRagioni.setLayoutParams(spinnerParams);
        spnRagioni.setPadding(20, 0, 0, 0);
        spnRagioni.setAdapter(adapter);
        spnRagioni.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                ragione = pos;
            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        linearLayout.addView(spnRagioni);

        EditText editTextDettagli = new EditText(callingContext);
        LinearLayout.LayoutParams etParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        etParams.topMargin = 30;
        etParams.bottomMargin = 20;
        editTextDettagli.setLayoutParams(etParams);
        editTextDettagli.setBackgroundResource(R.drawable.edit_text_background);
        editTextDettagli.setHint(getString(R.string.hintDettagli));
        editTextDettagli.setLines(4);
        editTextDettagli.setGravity(Gravity.TOP | Gravity.START);
        editTextDettagli.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        editTextDettagli.setPadding(16, 16, 16, 16);
        editTextDettagli.setHintTextColor(ContextCompat.getColor(requireContext(), R.color.grey));
        editTextDettagli.setTextColor(ContextCompat.getColor(requireContext(), R.color.tertiaryColor));
        editTextDettagli.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        linearLayout.addView(editTextDettagli);

        Button btnInvia = new Button(callingContext);
        btnInvia.setText(getString(R.string.invia));
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnParams.gravity = END;
        btnInvia.setLayoutParams(btnParams);
        btnInvia.setTextSize(11);
        btnInvia.setPadding(btnInvia.getPaddingLeft(), 0, btnInvia.getPaddingRight(), 0);
        btnInvia.setMinimumHeight((int) (32 * Resources.getSystem().getDisplayMetrics().density));
        btnInvia.setMinHeight((int) (32 * Resources.getSystem().getDisplayMetrics().density));
        btnInvia.setTextColor(ContextCompat.getColor(requireContext(), R.color.red));
        btnInvia.setBackgroundResource(R.drawable.background_button_report);
        btnInvia.setOnClickListener(v -> {
            analytics.send("App Event", "Segnala Avaria");
            String dettagli = editTextDettagli.getText().toString().replaceAll("\r\n|\r|\n", " ");
            scriviSegnalazione(true, dettagli);
            Toast.makeText(v.getContext(), R.string.ringraziamentoSegnalazione, Toast.LENGTH_SHORT).show();
            toggleReportGrid();
            updateButtonStates(callingActivity.getString(R.string.confermaOSmentisci));
            if (onReportListener != null) {
                onReportListener.onDialogDismissed();
            }
            dismiss();
        });

        linearLayout.addView(btnInvia);

        return linearLayout;
    }


    private boolean scriviSegnalazione(boolean problema, String dettagli) {
        try {
            int reason = problema ? ragione : Alert.REASON_NO_PROBLEM;
            LocalDate transportDate = LocalDate.of(calen.get(Calendar.YEAR), calen.get(Calendar.MONTH) + 1, calen.get(Calendar.DAY_OF_MONTH));

            if (mezzo.getGiornoSeguente())
                transportDate = transportDate.plusDays(1);

            Alert alert = new Alert(mezzo.getId(), reason, dettagli, transportDate);
            alertsDAO.send(alert);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Dynamically changes the color of the selected button among report, ticket, and taxi; only one can be selected at a time.
    private void updateButtonStates(String activeType) {
        if (callingActivity.getString(R.string.numeriTaxi).equals(activeType) && currentDynamicView != null && callingActivity.getString(R.string.numeriTaxi).equals(currentDynamicView.getTag())) {
            btnTaxi.setAlpha(0.5f);
        } else {
            btnTaxi.setAlpha(1.0f);
        }
        if (callingActivity.getString(R.string.numeriUtili).equals(activeType) && currentDynamicView != null && callingActivity.getString(R.string.numeriUtili).equals(currentDynamicView.getTag())) {
            btnBiglietterie.setAlpha(0.5f);
        } else {
            btnBiglietterie.setAlpha(1.0f);
        }
        if (callingActivity.getString(R.string.confermaOSmentisci).equals(activeType) && currentDynamicView != null && callingActivity.getString(R.string.confermaOSmentisci).equals(currentDynamicView.getTag())) {
            buttonSegnala.setAlpha(0.5f);
        } else {
            buttonSegnala.setAlpha(1.0f);
        }
    }

    private String getWeatherConditionsString(Context context, Mezzo route, Calendar calen) {
        double extraWind = meteo.getForecast(context, route, calen);

        if (extraWind <= 0)
            return "";
        else if (extraWind <= 1)
            return " - " + context.getString(R.string.pocoProbabile);
        else if (extraWind <= 2)
            return " - " + context.getString(R.string.aRischio);
        else if (extraWind <= 3)
            return " - " + context.getString(R.string.corsaQuasi);
        else
            return " - " + context.getString(R.string.corsaImpossibile);
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);

        // Imposta reportShortcut a false
        reportShortcut = false;

        // Distrugge il report grid se esiste
        if (currentDynamicView != null) {
            linear_layout_dettagli_mezzo.removeView(currentDynamicView);
            currentDynamicView = null;
        }

    }

}
