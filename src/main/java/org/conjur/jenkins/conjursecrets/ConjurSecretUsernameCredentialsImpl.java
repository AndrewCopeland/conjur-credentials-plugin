package org.conjur.jenkins.conjursecrets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.conjur.jenkins.configuration.ConjurConfiguration;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.cloudbees.jenkins.plugins.sshcredentials.SSHUserPrivateKey;
import com.cloudbees.plugins.credentials.CredentialsDescriptor;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import hudson.Extension;
import hudson.model.Item;
import hudson.model.Run;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import jenkins.model.Jenkins;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;

public class ConjurSecretUsernameCredentialsImpl extends BaseStandardCredentials
		implements ConjurSecretUsernameCredentials, SSHUserPrivateKey {

	private static final Logger LOGGER = Logger.getLogger(ConjurSecretUsernameCredentialsImpl.class.getName());

	private String username;
	private String credentialID;
	private ConjurConfiguration conjurConfiguration;
	private Secret passphrase;

	transient Run<?, ?> context;

	@DataBoundConstructor
	public ConjurSecretUsernameCredentialsImpl(CredentialsScope scope, String id, String username, String credentialID,
			ConjurConfiguration conjurConfiguration, Secret passphrase, String description) {
		super(scope, id, description);
		this.username = username;
		this.credentialID = credentialID;
		this.passphrase = passphrase;
		this.conjurConfiguration = conjurConfiguration;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public String getUsername() {
		LOGGER.log(Level.INFO, "Get UserName => {0}", this.username);
		return this.username;
	}

	@DataBoundSetter
	public void setUserName(String username) {
		this.username = username;
	}

	public String getCredentialID() {
		return credentialID;
	}

	@DataBoundSetter
	public void setCredentialID(String credentialID) {
		this.credentialID = credentialID;
	}

	public ConjurConfiguration getConjurConfiguration() {
		return conjurConfiguration;
	}

	@DataBoundSetter
	public void setConjurConfiguration(ConjurConfiguration conjurConfiguration) {
		this.conjurConfiguration = conjurConfiguration;
		ConjurSecretCredentials credential = CredentialsMatchers.firstOrNull(
				CredentialsProvider.lookupCredentials(ConjurSecretCredentials.class, Jenkins.getInstance(), ACL.SYSTEM,
						Collections.<DomainRequirement>emptyList()),
				CredentialsMatchers.withId(this.getCredentialID()));
		if (credential != null)
			credential.setConjurConfiguration(conjurConfiguration);

	}
	
	@Override
    public Secret getPassphrase() {
        return passphrase;
    }
	
	@DataBoundSetter
	public void setPassphrase(Secret passphrase) {
		this.passphrase = passphrase;
	}


	@Extension
	public static class DescriptorImpl extends CredentialsDescriptor {

		@Override
		public String getDisplayName() {
			return "Conjur Secret Username Credential";
		}

		public ListBoxModel doFillCredentialIDItems(@AncestorInPath Item item, @QueryParameter String uri) {
			return new StandardListBoxModel().includeAs(ACL.SYSTEM, item, ConjurSecretCredentials.class,
					URIRequirementBuilder.fromUri(uri).build());
		}

	}

	@Override
	public String getDisplayName() {
		return "ConjurSecretUsername:" + this.username;
	}

	@Override
	public void setContext(Run<?, ?> context) {
		LOGGER.log(Level.INFO, "Set Context");
		this.context = context;
	}

	@Override
	public Secret getSecret() {

		ConjurSecretCredentials credential = CredentialsMatchers.firstOrNull(
				CredentialsProvider.lookupCredentials(ConjurSecretCredentials.class, Jenkins.getInstance(), ACL.SYSTEM,
						Collections.<DomainRequirement>emptyList()),
				CredentialsMatchers.withId(this.getCredentialID()));

		Secret secret = null;

		if (credential != null) {
			if (conjurConfiguration != null)
				credential.setConjurConfiguration(conjurConfiguration);
			if (context != null)
				credential.setContext(context);
			secret = credential.getSecret();
		}

		return secret;
	}

	@Override
	public Secret getPassword() {
		LOGGER.log(Level.INFO, "Getting Password");
		return getSecret();
	}

	@Override
	public String getPrivateKey() {
		LOGGER.log(Level.INFO, "Getting SSH Key secret from Conjur");
		return getSecret().getPlainText();
	}

	@Override
	public List<String> getPrivateKeys() {
		List<String> result = new ArrayList<String>();
		result.add(getPrivateKey());
		return result;
	}

}
