package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val dal: AntaeusDal
) {

    private fun processInvoice(invoice: Invoice): Invoice {
        if (invoice.status == InvoiceStatus.PENDING) try {
            val success = paymentProvider.charge(invoice)
            if (success) {
                dal.updateInvoiceStatus(invoice.id, InvoiceStatus.PAID)
            } else {
                dal.updateInvoiceStatus(invoice.id, InvoiceStatus.DELINQUENT)
            }
        } catch (e: Exception) {
            when (e) {
                is CurrencyMismatchException, is CustomerNotFoundException, is NetworkException -> {
                    // When either of these exceptions occur, we simply add an error status and continue.
                    // Future implementations can include:
                    // CurrencyMismatchException: A Currency Calculator that can invalidate the current
                    // invoice and create a new one with the correct currency
                    // NetworkException: Retry before moving on
                    dal.updateInvoiceStatus(invoice.id, InvoiceStatus.ERROR)
                } else -> throw e
            }
        }
        return invoice
    }

    fun processSingle(id: Int): Boolean {
        return try {
            val invoice = dal.fetchInvoice(id)
            processInvoice(invoice!!)
            true
        } catch (e: InvoiceNotFoundException) {
            false
        }
    }

    // Processes all pending invoices in the database
    // Returns a list of the processed invoices, regardless of whether a charge was successful.
    fun processAll(): List<Invoice> {
        var processedInvoices: MutableList<Invoice> = ArrayList()
        val invoices: List<Invoice> = dal.fetchInvoices()

        invoices.forEach { invoice ->
            processedInvoices + processInvoice(invoice)
            processedInvoices + invoice
        }

        return processedInvoices
    }
}
