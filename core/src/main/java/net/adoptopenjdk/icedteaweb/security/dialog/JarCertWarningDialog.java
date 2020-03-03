package net.adoptopenjdk.icedteaweb.security.dialog;

import net.adoptopenjdk.icedteaweb.client.util.gridbag.GridBagPanelBuilder;
import net.adoptopenjdk.icedteaweb.i18n.Translator;
import net.adoptopenjdk.icedteaweb.image.ImageGallery;
import net.adoptopenjdk.icedteaweb.logging.Logger;
import net.adoptopenjdk.icedteaweb.logging.LoggerFactory;
import net.adoptopenjdk.icedteaweb.security.dialog.panel.LayoutPartsBuilder;
import net.adoptopenjdk.icedteaweb.security.dialog.result.AccessWarningResult;
import net.adoptopenjdk.icedteaweb.ui.dialogs.DialogButton;
import net.sourceforge.jnlp.JNLPFile;
import net.sourceforge.jnlp.runtime.SecurityDelegate;
import net.sourceforge.jnlp.security.AccessType;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.List;

/**
 * TODO: check if advancedOptions (temporary permissions) should still be supported
*/
public class JarCertWarningDialog extends CertWarningDialog {
    private static final Logger LOG = LoggerFactory.getLogger(JarCertWarningDialog.class);
    private static final Translator TRANSLATOR = Translator.getInstance();

    private final DialogButton<AccessWarningResult> runButton;
    private final DialogButton<AccessWarningResult> sandboxButton;
    private final DialogButton<AccessWarningResult> cancelButton;
    private final JButton advancedOptionsButton;

    private final boolean rootInCaCerts;
    private final AccessType accessType;
    private final SecurityDelegate securityDelegate;
    private final JNLPFile file;
    private boolean alwaysTrustSelected;

    protected JarCertWarningDialog(final String message, final AccessType accessType, final JNLPFile file, final boolean rootInCaCerts, final List<? extends Certificate> certificates, final List<String> certIssues, final SecurityDelegate securityDelegate) {
        super(message, file, certificates, certIssues, accessType == AccessType.VERIFIED);
        this.file = file;
        this.accessType = accessType;
        this.securityDelegate = securityDelegate;
        this.alwaysTrustSelected = (accessType == AccessType.VERIFIED);
        this.rootInCaCerts = rootInCaCerts;

        runButton = ButtonFactory.createRunButton(() -> AccessWarningResult.YES);
        sandboxButton = ButtonFactory.createSandboxButton(() -> AccessWarningResult.SANDBOX);
        sandboxButton.setEnabled(!alwaysTrustSelected);
        cancelButton = ButtonFactory.createCancelButton(TRANSLATOR.translate("CertWarnCancelTip"), () -> AccessWarningResult.NO);
        advancedOptionsButton = createAdvancedOptionsButton();
    }

    public static JarCertWarningDialog create(final AccessType accessType, final JNLPFile jnlpFile, final boolean rootInCaCerts, final List<? extends Certificate> certificates, final List<String> certIssues, final SecurityDelegate securityDelegate) {
        final String message = getMessageFor(accessType);
        return new JarCertWarningDialog(message, accessType, jnlpFile, rootInCaCerts, certificates, certIssues, securityDelegate);
    }

    @Override
    protected ImageIcon createIcon() {
        return alwaysTrustSelected ? ImageGallery.QUESTION.asImageIcon() : ImageGallery.WARNING.asImageIcon();
    }

    @Override
    protected JComponent createDetailPaneContent() {
        final GridBagPanelBuilder gridBuilder = new GridBagPanelBuilder();
        try {
            gridBuilder.addRows(LayoutPartsBuilder.getApplicationDetails(file));
            gridBuilder.addHorizontalSpacer();

            gridBuilder.addComponentRow(createMoreInformationPanel());

            gridBuilder.addHorizontalSpacer();

            gridBuilder.addComponentRow(createAlwaysTrustCheckbox());

        } catch (final Exception e) {
            LOG.error("Error while trying to read properties for CertWarningDialog!", e);
        }
        return gridBuilder.createGrid();
    }

    @Override
    protected List<DialogButton<AccessWarningResult>> createButtons() {
        return Arrays.asList(runButton, sandboxButton, cancelButton);
    }

    private JButton createAdvancedOptionsButton() {
        JButton advancedOptions = new JButton("\u2630");
        advancedOptions.setEnabled(file != null && securityDelegate != null);
        advancedOptions.setToolTipText(TRANSLATOR.translate("CertWarnPolicyTip"));
        return advancedOptions;
    }

    protected JCheckBox createAlwaysTrustCheckbox() {
        JCheckBox alwaysTrustCheckBox = super.createAlwaysTrustCheckbox();
        alwaysTrustCheckBox.addActionListener(e -> sandboxButton.setEnabled(alwaysTrustSelected = !alwaysTrustCheckBox.isSelected()));
        return alwaysTrustCheckBox;
    }

    @Override
    protected String getMoreInformationText() {
        String moreInformationText = rootInCaCerts ?
                TRANSLATOR.translate("STrustedSource") : TRANSLATOR.translate("SUntrustedSource");

        switch (accessType) {
            case UNVERIFIED:
            case SIGNING_ERROR:
                return moreInformationText + " " + TRANSLATOR.translate("SWarnFullPermissionsIgnorePolicy");
            default:
                return moreInformationText;
        }
    }

    private static String getMessageFor(final AccessType accessType) {
        switch (accessType) {
            case VERIFIED:
                return TRANSLATOR.translate("SSigVerified");
            case UNVERIFIED:
                return TRANSLATOR.translate("SSigUnverified");
            case SIGNING_ERROR:
                return TRANSLATOR.translate("SSignatureError");
            default:
                return "";
        }
    }
}
