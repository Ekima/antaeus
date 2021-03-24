package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import java.time.LocalDateTime
import java.util.Date
import java.util.Timer
import java.util.TimerTask

class InvoiceResultWrapper (
    val data: Invoice? = null,
    val errorMessage: String? = null
)

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val dal: AntaeusDal
) {
    private fun processPending(invoice: Invoice): Invoice {
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
                    dal.updateInvoiceStatus(invoice.id, InvoiceStatus.ERROR)
                } else -> throw e
            }
        }
        return invoice
    }

    fun processSingle(id: Int): InvoiceResultWrapper {
        // Process a single pending invoice in the database.
        // Returns either the updated invoice or an error.
        return try {
            val invoice = dal.fetchInvoice(id)
            processPending(invoice!!)
            InvoiceResultWrapper(data = dal.fetchInvoice(id))
        } catch (e: InvoiceNotFoundException) {
            InvoiceResultWrapper(errorMessage = "Invoice not found in database.")
        }
    }

    fun processAll(): List<Invoice> {
        // Process all pending invoices in the database.
        // Returns a list of all the invoices in the database.
        val invoices: List<Invoice> = dal.fetchInvoices()

        invoices.forEach { invoice ->
            processPending(invoice)
        }

        return dal.fetchInvoices()
    }

    fun initiateMonthlyUpdate() {
        val timer = Timer()
        val date = LocalDateTime.now()
        timer.schedule(object: TimerTask() {
            override fun run() {
                processAll()
                initiateMonthlyUpdate()
            }
        }, Date(date.year, date.monthValue + 1, 1, 0, 0))
    }
}
