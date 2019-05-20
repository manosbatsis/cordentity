package com.luxoft.blockchainlab.corda.hyperledger.indy.flow

import co.paralleluniverse.fibers.Suspendable
import com.luxoft.blockchainlab.corda.hyperledger.indy.contract.IndyCredentialContract
import com.luxoft.blockchainlab.corda.hyperledger.indy.data.state.getIndyCredentialState
import net.corda.core.contracts.Command
import net.corda.core.flows.*
import net.corda.core.transactions.TransactionBuilder


/**
 * Flow to revoke previously issued credential
 */
object RevokeCredentialFlow {

    /**
     * @param id           credential id generated by [issueCredentialFlow]
     */
    @InitiatingFlow
    @StartableByRPC
    open class Issuer(private val id: String) : FlowLogic<Unit>() {

        @Suspendable
        override fun call() {
            try {
                // query vault for credential with id = credential id
                val credentialStateIn = getIndyCredentialState(id)
                    ?: throw RuntimeException("No such credential in vault")

                val credential = credentialStateIn.state.data

                val revRegId = credential.credentialInfo.credential.getRevocationRegistryIdObject()!!
                val credRevId = credential.credentialInfo.credRevocId!!

                // revoke that credential
                indyUser().revokeCredentialAndUpdateLedger(revRegId, credRevId)

                val commandType = IndyCredentialContract.Command.Revoke()
                val signers = listOf(ourIdentity.owningKey)
                val command = Command(commandType, signers)

                val trxBuilder = TransactionBuilder(whoIsNotary())
                    .withItems(credentialStateIn, command)

                trxBuilder.toWireTransaction(serviceHub)
                    .toLedgerTransaction(serviceHub)
                    .verify()

                val selfSignedTx = serviceHub.signInitialTransaction(trxBuilder, ourIdentity.owningKey)

                subFlow(FinalityFlow(selfSignedTx))

            } catch (ex: Exception) {
                logger.error("", ex)
                throw FlowException(ex.message)
            }
        }
    }
}
