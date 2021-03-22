package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val dal: AntaeusDal
) {
    // Processes all pending invoices in the database
    // Returns the number of invoices paid vs. the total number of invoices
    // This should really be redesigned in a few ways. The invoicing system should
    // allow for a monthly report of pending payments, as well as a new state for
    // delinquent payments that can be taken out of the report and handed over to
    // the sales rep to follow up on payment.
    // For now, we return the invoices we failed billing in a list.
    fun processPending(): List<Invoice> {
        var failedInvoices: MutableList<Invoice> = ArrayList()
        val invoices: List<Invoice> = dal.fetchInvoices()

        invoices.forEach { invoice ->
            var success = false
            if (invoice.status == InvoiceStatus.PENDING) try {
                success = this.paymentProvider.charge(invoice)
            } catch (e: Exception) {
                when (e) {
                    is CurrencyMismatchException, is CustomerNotFoundException, is NetworkException -> {
                        // TODO: Create updateValue function in Invoice Service
                        // If CurrencyMismatch, skip. The customer might have changed currencies.
                        // Explain in README why this is a problem. Solve with an update to the invoice.
                        // If we have a CustomerNotFound, we skip as well for now.
                        // If we have a NetworkException, initiate a retry strategy.
                    }
                    else -> throw e
                }
            }
            if (success) {
                //Update Invoice Status to PAID
                // TODO: Create updateStatus function in Invoice Service
            } else {
                failedInvoices + invoice
            }
        }

        return failedInvoices
    }
}
