package com.luxoft.blockchainlab.hyperledger.indy

import com.luxoft.blockchainlab.hyperledger.indy.helpers.GenesisHelper
import com.luxoft.blockchainlab.hyperledger.indy.helpers.PoolHelper
import com.luxoft.blockchainlab.hyperledger.indy.helpers.WalletHelper
import com.luxoft.blockchainlab.hyperledger.indy.ledger.IndyPoolLedgerUser
import com.luxoft.blockchainlab.hyperledger.indy.models.*
import com.luxoft.blockchainlab.hyperledger.indy.wallet.IndySDKWalletUser
import junit.framework.Assert.assertFalse
import org.hyperledger.indy.sdk.did.Did
import org.hyperledger.indy.sdk.did.DidResults
import org.hyperledger.indy.sdk.ledger.Ledger
import org.hyperledger.indy.sdk.pool.Pool
import org.hyperledger.indy.sdk.wallet.Wallet
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.io.File


class AnoncredsDemoTest : IndyIntegrationTest() {
    private val walletPassword = "password"
    private val issuerWalletName = "issuerWallet"
    private val issuer2WalletName = "issuer2Wallet"
    private val proverWalletName = "proverWallet"

    private lateinit var issuerWallet: Wallet
    private lateinit var issuer1: SsiUser

    private lateinit var issuer2Wallet: Wallet
    private lateinit var issuer2: SsiUser

    private lateinit var proverWallet: Wallet
    private lateinit var prover: SsiUser

    companion object {
        private lateinit var pool: Pool
        private lateinit var poolName: String

        @JvmStatic
        @BeforeClass
        fun setUpTest() {
            System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "TRACE")

            // Create and Open Pool
            poolName = PoolHelper.DEFAULT_POOL_NAME
            val genesisFile = File(TEST_GENESIS_FILE_PATH)
            if (!GenesisHelper.exists(genesisFile))
                throw RuntimeException("Genesis file $TEST_GENESIS_FILE_PATH doesn't exist")

            PoolHelper.createOrTrunc(genesisFile, poolName)
            pool = PoolHelper.openExisting(poolName)
        }

        @JvmStatic
        @AfterClass
        fun tearDownTest() {
            // Close pool
            pool.closePoolLedger().get()
            Pool.deletePoolLedgerConfig(poolName)
        }
    }

    @Before
    @Throws(Exception::class)
    fun setUp() {
        // create and open wallets
        WalletHelper.createOrTrunc("Trustee", "123")
        val trusteeWallet = WalletHelper.openExisting("Trustee", "123")

        WalletHelper.createOrTrunc(issuerWalletName, walletPassword)
        issuerWallet = WalletHelper.openExisting(issuerWalletName, walletPassword)

        WalletHelper.createOrTrunc(issuer2WalletName, walletPassword)
        issuer2Wallet = WalletHelper.openExisting(issuer2WalletName, walletPassword)

        WalletHelper.createOrTrunc(proverWalletName, walletPassword)
        proverWallet = WalletHelper.openExisting(proverWalletName, walletPassword)

        // create trustee did
        val trusteeDidInfo = createTrusteeDid(trusteeWallet)

        // create indy users
        val issuerWalletUser = IndySDKWalletUser(issuerWallet)
        val issuerLedgerUser = IndyPoolLedgerUser(pool, issuerWalletUser.did) { issuerWalletUser.sign(it) }
        issuer1 = IndyUser.with(issuerWalletUser).with(issuerLedgerUser).build()

        val issuer2WalletUser = IndySDKWalletUser(issuer2Wallet)
        val issuer2LedgerUser = IndyPoolLedgerUser(pool, issuer2WalletUser.did) { issuer2WalletUser.sign(it) }
        issuer2 = IndyUser.with(issuer2LedgerUser).with(issuer2WalletUser).build()

        val proverWalletUser = IndySDKWalletUser(proverWallet)
        val proverLedgerUser = IndyPoolLedgerUser(pool, proverWalletUser.did) { proverWalletUser.sign(it) }
        prover = IndyUser.with(proverLedgerUser).with(proverWalletUser).build()

        // set relationships
        linkIssuerToTrustee(trusteeWallet, trusteeDidInfo, issuerWalletUser.getIdentityDetails())
        linkIssuerToTrustee(trusteeWallet, trusteeDidInfo, issuer2WalletUser.getIdentityDetails())

        issuer1.addKnownIdentitiesAndStoreOnLedger(prover.walletUser.getIdentityDetails())

        trusteeWallet.closeWallet().get()
    }

    private fun linkIssuerToTrustee(
        trusteeWallet: Wallet,
        trusteeDidInfo: DidResults.CreateAndStoreMyDidResult,
        issuerDidInfo: IdentityDetails
    ) {
        val nymRequest = Ledger.buildNymRequest(
            trusteeDidInfo.did,
            issuerDidInfo.did,
            issuerDidInfo.verkey,
            null,
            "TRUSTEE"
        ).get()

        Ledger.signAndSubmitRequest(pool, trusteeWallet, trusteeDidInfo.did, nymRequest).get()
    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
        // Issuer Remove Wallet
        issuerWallet.closeWallet().get()
        issuer2Wallet.closeWallet().get()

        // Prover Remove Wallet
        proverWallet.closeWallet().get()
    }

    private fun createTrusteeDid(wallet: Wallet) = Did.createAndStoreMyDid(wallet, """{"seed":"$TRUSTEE_SEED"}""").get()

    @Test
    @Throws(Exception::class)
    fun `revocation works fine`() {
        val gvtSchema = issuer1.createSchemaAndStoreOnLedger(GVT_SCHEMA_NAME, SCHEMA_VERSION, GVT_SCHEMA_ATTRIBUTES)
        val credDef = issuer1.createCredentialDefinitionAndStoreOnLedger(gvtSchema.getSchemaIdObject(), true)
        val revocationRegistry =
            issuer1.createRevocationRegistryAndStoreOnLedger(credDef.getCredentialDefinitionIdObject(), 5)

        val credOffer = issuer1.createCredentialOffer(credDef.getCredentialDefinitionIdObject())
        val credReq = prover.createCredentialRequest(prover.walletUser.getIdentityDetails().did, credOffer)
        val credentialInfo = issuer1.issueCredentialAndUpdateLedger(
            credReq,
            credOffer,
            revocationRegistry.definition.getRevocationRegistryIdObject()
        ) {
            mapOf(
                "sex" to CredentialValue("male"),
                "name" to CredentialValue("Alex"),
                "height" to CredentialValue("175"),
                "age" to CredentialValue("28")
            )
        }
        prover.checkLedgerAndReceiveCredential(credentialInfo, credReq, credOffer)

        Thread.sleep(3000)

        val fieldName = CredentialFieldReference("name", gvtSchema.id, credDef.id)
        val fieldSex = CredentialFieldReference("sex", gvtSchema.id, credDef.id)
        val fieldAge = CredentialFieldReference("age", gvtSchema.id, credDef.id)

        val proofReq = issuer1.createProofRequest(
            version = "0.1",
            name = "proof_req_0.1",
            attributes = listOf(fieldName, fieldSex),
            predicates = listOf(CredentialPredicate(fieldAge, 18)),
            nonRevoked = Interval.now(),
            nonce = "1"
        )

        val proof = prover.createProofFromLedgerData(proofReq)

        assertEquals("Alex", proof["name"]!!.raw)
        assertTrue(issuer1.verifyProofWithLedgerData(proofReq, proof))

        issuer1.revokeCredentialAndUpdateLedger(
            credentialInfo.credential.getRevocationRegistryIdObject()!!,
            credentialInfo.credRevocId!!
        )
        Thread.sleep(3000)

        val proofReqAfterRevocation = issuer1.createProofRequest(
            version = "0.1",
            name = "proof_req_0.1",
            attributes = listOf(fieldName, fieldSex),
            predicates = listOf(CredentialPredicate(fieldAge, 18)),
            nonRevoked = Interval.now(),
            nonce = "2"
        )
        val proofAfterRevocation = prover.createProofFromLedgerData(proofReqAfterRevocation)

        assertFalse(issuer1.verifyProofWithLedgerData(proofReqAfterRevocation, proofAfterRevocation))
    }

    @Test
    @Throws(Exception::class)
    fun `1 issuer 1 prover 1 credential setup works fine`() {
        val gvtSchema = issuer1.createSchemaAndStoreOnLedger(GVT_SCHEMA_NAME, SCHEMA_VERSION, GVT_SCHEMA_ATTRIBUTES)
        val credDef = issuer1.createCredentialDefinitionAndStoreOnLedger(gvtSchema.getSchemaIdObject(), false)
        val credOffer = issuer1.createCredentialOffer(credDef.getCredentialDefinitionIdObject())
        val credReq = prover.createCredentialRequest(prover.walletUser.getIdentityDetails().did, credOffer)
        val credentialInfo = issuer1.issueCredentialAndUpdateLedger(
            credReq,
            credOffer,
           null
        ) {
            mapOf(
                "sex" to CredentialValue("male"),
                "name" to CredentialValue("Alex"),
                "height" to CredentialValue("175"),
                "age" to CredentialValue("28")
            )
        }
        prover.checkLedgerAndReceiveCredential(credentialInfo, credReq, credOffer)

        val fieldName = CredentialFieldReference("name", gvtSchema.id, credDef.id)
        val fieldSex = CredentialFieldReference("sex", gvtSchema.id, credDef.id)
        val fieldAge = CredentialFieldReference("age", gvtSchema.id, credDef.id)
        val proofReq = issuer1.createProofRequest(
            version = "0.1",
            name = "proof_req_0.1",
            attributes = listOf(fieldName, fieldSex),
            predicates = listOf(CredentialPredicate(fieldAge, 18)),
            nonRevoked = null
        )

        val proof = prover.createProofFromLedgerData(proofReq)

        assertEquals("Alex", proof["name"]!!.raw)
        assertTrue(issuer1.verifyProofWithLedgerData(proofReq, proof))
    }

    @Test
    @Throws(Exception::class)
    fun `2 issuers 1 prover 2 credentials setup works fine`() {
        val schema1 = issuer1.createSchemaAndStoreOnLedger(GVT_SCHEMA_NAME, SCHEMA_VERSION, GVT_SCHEMA_ATTRIBUTES)
        val credDef1 = issuer1.createCredentialDefinitionAndStoreOnLedger(schema1.getSchemaIdObject(), false)

        val schema2 = issuer2.createSchemaAndStoreOnLedger(XYZ_SCHEMA_NAME, SCHEMA_VERSION, XYZ_SCHEMA_ATTRIBUTES)
        val credDef2 = issuer2.createCredentialDefinitionAndStoreOnLedger(schema2.getSchemaIdObject(), false)
        val gvtCredOffer = issuer1.createCredentialOffer(credDef1.getCredentialDefinitionIdObject())
        val xyzCredOffer = issuer2.createCredentialOffer(credDef2.getCredentialDefinitionIdObject())

        val gvtCredReq = prover.createCredentialRequest(prover.walletUser.getIdentityDetails().did, gvtCredOffer)
        val gvtCredential = issuer1.issueCredentialAndUpdateLedger(gvtCredReq, gvtCredOffer, null) {
            mapOf(
                "sex" to CredentialValue("male"),
                "name" to CredentialValue("Alex"),
                "height" to CredentialValue("175"),
                "age" to CredentialValue("28")
            )
        }
        prover.checkLedgerAndReceiveCredential(gvtCredential, gvtCredReq, gvtCredOffer)

        val xyzCredReq = prover.createCredentialRequest(prover.walletUser.getIdentityDetails().did, xyzCredOffer)
        val xyzCredential = issuer2.issueCredentialAndUpdateLedger(xyzCredReq, xyzCredOffer, null) {
            mapOf(
                "status" to CredentialValue("partial"),
                "period" to CredentialValue("8")
            )
        }
        prover.checkLedgerAndReceiveCredential(xyzCredential, xyzCredReq, xyzCredOffer)

        val field_name = CredentialFieldReference("name", schema1.id, credDef1.id)
        val field_age = CredentialFieldReference("age", schema1.id, credDef1.id)
        val field_status = CredentialFieldReference("status", schema2.id, credDef2.id)
        val field_period = CredentialFieldReference("period", schema2.id, credDef2.id)

        val proofReq = issuer1.createProofRequest(
            version = "0.1",
            name = "proof_req_0.1",
            attributes = listOf(field_name, field_status),
            predicates = listOf(CredentialPredicate(field_age, 18), CredentialPredicate(field_period, 5)),
            nonRevoked = null
        )

        val proof = prover.createProofFromLedgerData(proofReq)

        // Verifier verify Proof
        val revealedAttr0 = proof["name"]!!
        assertEquals("Alex", revealedAttr0.raw)

        val revealedAttr1 = proof["status"]!!
        assertEquals("partial", revealedAttr1.raw)

        assertTrue(issuer1.verifyProofWithLedgerData(proofReq, proof))
    }

    @Test
    @Throws(Exception::class)
    fun `1 issuer 1 prover 2 credentials setup works fine`() {
        val gvtSchema = issuer1.createSchemaAndStoreOnLedger(GVT_SCHEMA_NAME, SCHEMA_VERSION, GVT_SCHEMA_ATTRIBUTES)
        val gvtCredDef = issuer1.createCredentialDefinitionAndStoreOnLedger(gvtSchema.getSchemaIdObject(), false)

        val xyzSchema = issuer1.createSchemaAndStoreOnLedger(XYZ_SCHEMA_NAME, SCHEMA_VERSION, XYZ_SCHEMA_ATTRIBUTES)
        val xyzCredDef = issuer1.createCredentialDefinitionAndStoreOnLedger(xyzSchema.getSchemaIdObject(), false)
        val gvtCredOffer = issuer1.createCredentialOffer(gvtCredDef.getCredentialDefinitionIdObject())
        val xyzCredOffer = issuer1.createCredentialOffer(xyzCredDef.getCredentialDefinitionIdObject())

        val gvtCredReq = prover.createCredentialRequest(prover.walletUser.getIdentityDetails().did, gvtCredOffer)
        val gvtCredential = issuer1.issueCredentialAndUpdateLedger(gvtCredReq, gvtCredOffer, null) {
            mapOf(
                "sex" to CredentialValue("male"),
                "name" to CredentialValue("Alex"),
                "height" to CredentialValue("175"),
                "age" to CredentialValue("28")
            )
        }
        prover.checkLedgerAndReceiveCredential(gvtCredential, gvtCredReq, gvtCredOffer)

        val xyzCredReq = prover.createCredentialRequest(prover.walletUser.getIdentityDetails().did, xyzCredOffer)
        val xyzCredential = issuer1.issueCredentialAndUpdateLedger(xyzCredReq, xyzCredOffer, null) {
            mapOf(
                "status" to CredentialValue("partial"),
                "period" to CredentialValue("8")
            )
        }
        prover.checkLedgerAndReceiveCredential(xyzCredential, xyzCredReq, xyzCredOffer)

        val field_name = CredentialFieldReference("name", gvtSchema.id, gvtCredDef.id)
        val field_age = CredentialFieldReference("age", gvtSchema.id, gvtCredDef.id)
        val field_status = CredentialFieldReference("status", xyzSchema.id, xyzCredDef.id)
        val field_period = CredentialFieldReference("period", xyzSchema.id, xyzCredDef.id)

        val proofReq = issuer1.createProofRequest(
            version = "0.1",
            name = "proof_req_0.1",
            attributes = listOf(field_name, field_status),
            predicates = listOf(CredentialPredicate(field_age, 18), CredentialPredicate(field_period, 5)),
            nonRevoked = null
        )

        val proof = prover.createProofFromLedgerData(proofReq)

        // Verifier verify Proof
        val revealedAttr0 = proof["name"]!!
        assertEquals("Alex", revealedAttr0.raw)

        val revealedAttr1 = proof["status"]!!
        assertEquals("partial", revealedAttr1.raw)

        assertTrue(issuer1.verifyProofWithLedgerData(proofReq, proof))
    }
}
