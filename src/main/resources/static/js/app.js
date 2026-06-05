alert('JS LOADED');
const app = {
    token: localStorage.getItem('kumbuka_token'),
    user: null,
    baseUrl: '/api',

    init() {
        try {
            const userData = localStorage.getItem('kumbuka_user');
            if (userData) this.user = JSON.parse(userData);
        } catch (e) {
            console.error('Failed to parse user data', e);
            this.logout();
        }

        if (this.token && this.user) {
            this.showDashboard();
        } else {
            document.getElementById('auth-section').classList.remove('hidden');
        }
    },

    async login() {
        const email = document.getElementById('username').value;
        const password = document.getElementById('password').value;
        const errorEl = document.getElementById('auth-error');

        try {
            const response = await fetch(`${this.baseUrl}/auth/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, password })
            });

            const data = await response.json();

            if (data.token) {
                this.token = data.token;
                this.user = data;
                localStorage.setItem('kumbuka_token', data.token);
                localStorage.setItem('kumbuka_user', JSON.stringify(data));
                this.showDashboard();
            } else {
                errorEl.textContent = data.message || 'Login failed';
                errorEl.style.display = 'block';
            }
        } catch (err) {
            errorEl.textContent = 'Connection error';
            errorEl.style.display = 'block';
        }
    },

    logout() {
        localStorage.removeItem('kumbuka_token');
        localStorage.removeItem('kumbuka_user');
        window.location.reload();
    },

    showDashboard() {
        document.getElementById('auth-section').classList.add('hidden');
        document.getElementById('dashboard').classList.remove('hidden');
        document.getElementById('user-display').textContent = `Welcome, ${this.user.email}`;
        this.fetchData();
    },

    async fetchData() {
        console.log('Fetching data...');
        try {
            const [loans, txs] = await Promise.all([
                this.apiGet('/loans/all'),
                this.apiGet('/transactions/all')
            ]);

            console.log('Loans received:', loans);
            console.log('Transactions received:', txs);

            this.renderLoans(Array.isArray(loans) ? loans : []);
            this.renderTransactions(Array.isArray(txs) ? txs : []);
            this.updateStats(Array.isArray(loans) ? loans : [], Array.isArray(txs) ? txs : []);
        } catch (err) {
            console.error('Fetch error:', err);
        }
    },

    async apiGet(path) {
        try {
            const response = await fetch(`${this.baseUrl}${path}`, {
                headers: { 'Authorization': `Bearer ${this.token}` }
            });
            if (response.status === 403) {
                console.warn('Forbidden access, logging out...');
                this.logout();
                return [];
            }
            if (!response.ok) {
                console.error(`API Error: ${response.status}`);
                return [];
            }
            return await response.json();
        } catch (e) {
            console.error('Network or parsing error:', e);
            return [];
        }
    },

    renderLoans(loans) {
        const table = document.getElementById('loans-table');
        if (!table) return;
        if (loans.length === 0) {
            table.innerHTML = '<tr><td colspan="5" style="text-align:center;">No loans found</td></tr>';
            return;
        }
        table.innerHTML = loans.map(loan => {
            const borrowerName = (loan && loan.borrower && loan.borrower.email) ? loan.borrower.email : 'N/A';
            const amount = (loan && loan.amount) ? loan.amount : 0;
            const balance = (loan && loan.balance) ? loan.balance : 0;
            const status = (loan && loan.status) ? loan.status : 'UNKNOWN';
            
            return `
                <tr>
                    <td>#${loan.id || '?'}</td>
                    <td>${borrowerName}</td>
                    <td>KES ${amount.toLocaleString()}</td>
                    <td>KES ${balance.toLocaleString()}</td>
                    <td><span class="status-badge status-${status.toLowerCase()}">${status}</span></td>
                </tr>
            `;
        }).join('');
    },

    renderTransactions(txs) {
        const table = document.getElementById('transactions-table');
        if (!table) return;
        if (txs.length === 0) {
            table.innerHTML = '<tr><td colspan="3" style="text-align:center;">No transactions found</td></tr>';
            return;
        }
        table.innerHTML = txs.map(tx => {
            const mpesaId = (tx && tx.mpesaTransactionId) ? tx.mpesaTransactionId : 'N/A';
            const amount = (tx && tx.amount) ? tx.amount : 0;
            const status = (tx && tx.status) ? tx.status : 'UNKNOWN';

            return `
                <tr>
                    <td>${mpesaId}</td>
                    <td>KES ${amount.toLocaleString()}</td>
                    <td><span class="status-badge status-${status.toLowerCase()}">${status}</span></td>
                </tr>
            `;
        }).join('');
    },

    updateStats(loans, txs) {
        const loanCountEl = document.getElementById('stat-loans');
        const capitalEl = document.getElementById('stat-capital');
        const txCountEl = document.getElementById('stat-transactions');

        if (loanCountEl) loanCountEl.textContent = loans.length;
        
        const totalDisbursed = loans.reduce((sum, l) => sum + (l.amount || 0), 0);
        if (capitalEl) capitalEl.textContent = `KES ${totalDisbursed.toLocaleString()}`;
        
        const approvedCount = txs.filter(t => t && t.status === 'APPROVED').length;
        if (txCountEl) txCountEl.textContent = approvedCount;
    }
};

app.init();
