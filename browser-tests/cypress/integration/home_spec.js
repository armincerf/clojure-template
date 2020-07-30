const host = "localhost:5001"
const home = host + "/dashboard"
describe('Tables show data', () => {
    it('Customers page shows data', () => {
        cy.visit(home + '/customers')
        cy.get('.table__row > :nth-child(1)').should('be.visible')
    })

    it('Table updates when a new alert is POSTed', () => {
        cy.get('.table__body').find('tr').its('length').then(count_before => {
            cy.readFile('../docs/test-alert.edn').then(str => {
                cy.request({
                    url: host + '/api/iot-alerts',
                    headers: {
                        'Content-Type': 'application/edn'
                    },
                    method: "POST",
                    body: str
                });
            })
            //wait for sse event to come through
            cy.wait(500)
            cy.get('.table__body').find('tr').its('length').should('be.gt', count_before)
        })
    })

    it('Alerts page shows data', () => {
        cy.visit(home + '/alerts')
        cy.get('.table__row > :nth-child(1)').should('be.visible')
    })

    it('Can click through to dealership which also shows data', () => {
        cy.get('.table__row > :nth-child(1)').click()
        cy.get('.table__row > :nth-child(1)').should('be.visible')
        cy.url().should('contain', 'dealership/')
    })

    it('Can click through to customer which also shows data', () => {
        cy.get('.table__link').click()
        cy.get('.table__row > :nth-child(1)').should('be.visible')
        cy.url().should('contain', '/customer/')
    })

})
