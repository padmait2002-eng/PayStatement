import React, { useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';
import './styles.css';

let accessToken = '';
const API_BASE = 'http://localhost:8080/api';

const endpoint = async (path, options = {}) => {
  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      'X-Employee-Id': localStorage.getItem('employeeId') || 'local-employee',
      'X-User-Role': localStorage.getItem('role') || 'EMPLOYEE',
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      ...options.headers
    }
  });
  const body = response.status === 204 ? null : await response.json();
  if (!response.ok) throw new Error(body?.detail || body?.message || `Request failed (${response.status})`);
  return body;
};

function App() {
  const [role, setRole] = useState(localStorage.getItem('role') || 'EMPLOYEE');
  const [employeeId, setEmployeeId] = useState(localStorage.getItem('employeeId') || 'local-employee');
  const [token, setToken] = useState('');
  const [tab, setTab] = useState('statements');
  const [statements, setStatements] = useState([]);
  const [requests, setRequests] = useState([]);
  const [employees, setEmployees] = useState([]);
  const [taxes, setTaxes] = useState([]);
  const [deductions, setDeductions] = useState([]);
  const [payCodes, setPayCodes] = useState([]);
  const [query, setQuery] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const privileged = role === 'ADMIN' || role === 'PAYMENT_SPECIALIST';

  const run = async (action) => {
    setError('');
    try { await action(); }
    catch (e) { setError(e.message); }
  };
  const refresh = () => run(async () => {
    if (privileged) {
      const [people, taxRules, deductionRules, codeRules, specialistRequests] = await Promise.all([
        endpoint('/admin/employees'), endpoint('/admin/rules/taxes'),
        endpoint('/admin/rules/deductions'), endpoint('/admin/rules/pay-codes'),
        endpoint('/specialist/requests')
      ]);
      setEmployees(people); setTaxes(taxRules); setDeductions(deductionRules);
      setPayCodes(codeRules); setRequests(specialistRequests);
      if (people.some(person => person.employeeId === employeeId)) {
        setStatements(await endpoint(`/admin/statements?employeeId=${encodeURIComponent(employeeId)}`));
      } else {
        setStatements([]);
      }
    } else {
      const [items, ownRequests] = await Promise.all([endpoint('/statements'), endpoint('/requests/mine')]);
      setStatements(items); setRequests(ownRequests);
    }
  });
  useEffect(() => { refresh(); }, [employeeId, role, token]);

  const submitRequest = (event) => {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    run(async () => {
      await endpoint('/requests', { method: 'POST', body: JSON.stringify({
        weekEnding: data.get('weekEnding'), hours: Number(data.get('hours')),
        payCode: data.get('payCode'), explanation: data.get('explanation')
      }) });
      event.target.reset();
      setMessage('Change request submitted for payment specialist approval.');
      refresh();
    });
  };
  const saveEmployee = (event) => {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    run(async () => {
      await endpoint('/admin/employees', { method: 'POST', body: JSON.stringify({
        employeeId: data.get('employeeId'), name: data.get('name'), region: data.get('region'),
        incentives: Number(data.get('incentives') || 0), active: true
      }) });
      await endpoint(`/admin/employees/${encodeURIComponent(data.get('employeeId'))}/compensation`, {
        method: 'PUT', body: JSON.stringify({ regularHourlyRate: Number(data.get('rate')) })
      });
      setMessage('Employee and hourly rate saved.'); refresh();
    });
  };
  const saveHours = (event) => {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    run(async () => {
      await endpoint('/admin/weekly-hours', { method: 'POST', body: JSON.stringify({
        employeeId: data.get('employeeId'), weekEnding: data.get('weekEnding'), hoursWorked: Number(data.get('hours'))
      }) });
      setMessage('Weekly hours saved.');
    });
  };
  const saveRule = (event, type) => {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    const rule = { name: data.get('name'), kind: data.get('kind'), value: Number(data.get('value')), enabled: true };
    if (type === 'tax') Object.assign(rule, { scope: data.get('scope'), region: data.get('region') || null });
    run(async () => {
      await endpoint(`/admin/rules/${type === 'tax' ? 'taxes' : 'deductions'}`, { method: 'POST', body: JSON.stringify(rule) });
      event.target.reset(); setMessage('Pay rule saved.'); refresh();
    });
  };
  const savePayCode = (event) => {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    run(async () => {
      await endpoint('/admin/rules/pay-codes', { method: 'POST', body: JSON.stringify({
        payCode: data.get('payCode'), description: data.get('description'),
        multiplier: Number(data.get('multiplier')),
        countsTowardOvertime: data.get('countsTowardOvertime') === 'on', enabled: true
      }) });
      event.target.reset(); setMessage('Pay-code rule saved.'); refresh();
    });
  };
  const generate = (event) => {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    run(async () => {
      await endpoint('/statements/generate', { method: 'POST', body: JSON.stringify({
        employeeId: data.get('employeeId'), weekEnding: data.get('weekEnding')
      }) });
      setEmployeeId(data.get('employeeId'));
      localStorage.setItem('employeeId', data.get('employeeId'));
      setMessage('Statement generated and queued for Kafka notification when enabled.'); refresh();
    });
  };
  const decide = (id, approve) => run(async () => {
    await endpoint(`/specialist/requests/${id}/decision?approve=${approve}`, { method: 'POST' });
    setMessage(`Request ${approve ? 'approved and statement recalculated when weekly hours are available' : 'rejected'}.`);
    refresh();
  });
  const visibleStatements = statements.filter(s => !query || s.weekEnding.includes(query) || String(s.id).includes(query));

  return <div className="shell">
    <header className="topbar"><div className="brand"><span className="brand-mark">P</span><div><strong>Paystatement</strong><small>Employee pay center</small></div></div>
      <div className="identity"><input aria-label="Employee ID" value={employeeId} onChange={e => { setEmployeeId(e.target.value); localStorage.setItem('employeeId', e.target.value); }} />
        <select aria-label="Local role" value={role} onChange={e => { setRole(e.target.value); localStorage.setItem('role', e.target.value); }}>
          <option>EMPLOYEE</option><option>PAYMENT_SPECIALIST</option><option>ADMIN</option>
        </select>
        <input aria-label="ADFS bearer token" type="password" autoComplete="off" placeholder="ADFS token (dev/UAT/prod)" value={token}
          onChange={e => { accessToken = e.target.value; setToken(e.target.value); }} />
        <span className="avatar">{employeeId.slice(0, 1).toUpperCase()}</span></div>
    </header>
    <main>
      <section className="welcome"><div><p className="eyebrow">WEEKLY PAY</p><h1>Your pay, clearly stated.</h1><p>Search statements, request a pay detail change, or manage payroll settings.</p></div>
        <div className="welcome-icon">▤</div></section>
      {message && <div className="notice" role="status">{message}<button onClick={() => setMessage('')}>×</button></div>}
      {error && <div className="error" role="alert">{error}<button onClick={() => setError('')}>×</button></div>}
      <nav className="tabs">
        <button className={tab === 'statements' ? 'active' : ''} onClick={() => setTab('statements')}>Pay statements</button>
        <button className={tab === 'modify' ? 'active' : ''} onClick={() => setTab('modify')}>Request a change</button>
        {privileged && <><button className={tab === 'review' ? 'active' : ''} onClick={() => setTab('review')}>Approvals</button>
          <button className={tab === 'admin' ? 'active' : ''} onClick={() => setTab('admin')}>Admin setup</button></>}
      </nav>
      {tab === 'statements' && <section className="panel">
        <div className="panel-head"><div><h2>Pay statements</h2><p>Weekly earnings, taxes, deductions, and net pay.</p></div>
          <div className="search"><span>⌕</span><input placeholder={privileged ? "Filter selected employee's statements" : "Search by week ending or statement ID"} value={query} onChange={e => setQuery(e.target.value)} /></div></div>
        {privileged && <form className="inline-form" onSubmit={generate}><label>Employee<select name="employeeId" required>{employees.map(e => <option key={e.employeeId} value={e.employeeId}>{e.name} ({e.employeeId})</option>)}</select></label><label>Week ending<input name="weekEnding" type="date" required /></label><button className="primary">Generate statement</button></form>}
        <div className="table-wrap"><table><thead><tr><th>Week ending</th><th>Hours</th><th>Gross pay</th><th>Taxes</th><th>Deductions</th><th>Net pay</th><th>Status</th></tr></thead>
          <tbody>{visibleStatements.map(s => <tr key={s.id}><td>{s.weekEnding}</td><td>{Number(s.regularHours) + Number(s.overtimeHours) + Number(s.otherPaidHours || 0)} <small>({s.overtimeHours} OT, {s.otherPaidHours || 0} entitlement)</small></td><td>${Number(s.grossPay).toFixed(2)}</td><td>${Number(s.taxes).toFixed(2)}</td><td>${Number(s.deductions).toFixed(2)}</td><td className="net">${Number(s.netPay).toFixed(2)}</td><td><span className="badge">{s.status}</span></td></tr>)}
          {!visibleStatements.length && <tr><td colSpan="7" className="empty">No statements found. For local setup, add your employee in Admin setup.</td></tr>}</tbody></table></div>
      </section>}
      {tab === 'modify' && <section className="panel narrow"><h2>Request a pay change</h2><p>Submit additional hours or an entitlement/pay-code correction. A payment specialist must review it.</p>
        <form className="form-grid" onSubmit={submitRequest}><label>Week ending<input name="weekEnding" type="date" required /></label><label>Hours to add<input name="hours" type="number" min="0.01" step="0.01" required /></label>
          <label>Pay code<input name="payCode" placeholder="REGULAR, OVERTIME, SICK" required /></label><label className="span-two">Explanation<textarea name="explanation" rows="3" required placeholder="Describe the hours or entitlement that needs correction." /></label>
          <button className="primary">Submit for approval</button></form>
        <h3>Your requests</h3><RequestList requests={requests} /></section>}
      {tab === 'review' && <section className="panel"><h2>Specialist review queue</h2><p>Review requested hours and pay-code updates before recalculation.</p><RequestList requests={requests} actions={decide} /></section>}
      {tab === 'admin' && <section className="panel"><h2>Payroll configuration</h2><p>Set up employee pay rates, weekly hours, federal and regional tax rules, and deductions.</p>
        <div className="admin-grid">
          <form className="admin-card" onSubmit={saveEmployee}><h3>Employee & hourly rate</h3><label>Employee ID<input name="employeeId" required /></label><label>Full name<input name="name" required /></label><label>Region<select name="region"><option>NORTH</option><option>SOUTH</option><option>WEST</option><option>EAST</option><option>CENTRAL</option></select></label><label>Incentives ($; excluded from gross pay)<input name="incentives" type="number" min="0" step="0.01" defaultValue="0" /></label><label>Regular hourly rate ($)<input name="rate" type="number" min="0.01" step="0.01" required /></label><button className="primary">Save employee</button></form>
          <form className="admin-card" onSubmit={saveHours}><h3>Weekly hours worked</h3><label>Employee<select name="employeeId" required>{employees.map(e => <option key={e.employeeId} value={e.employeeId}>{e.name} ({e.employeeId})</option>)}</select></label><label>Week ending<input name="weekEnding" type="date" required /></label><label>Hours<input name="hours" type="number" min="0" max="168" step="0.01" required /></label><button className="primary">Save hours</button></form>
          <form className="admin-card" onSubmit={e => saveRule(e, 'tax')}><h3>Tax rule</h3><label>Rule name<input name="name" placeholder="Federal income tax" required /></label><label>Scope<select name="scope"><option>FEDERAL</option><option>REGION</option></select></label><label>Region (regional tax)<select name="region"><option value="">—</option><option>NORTH</option><option>SOUTH</option><option>WEST</option><option>EAST</option><option>CENTRAL</option></select></label><RuleValueFields /><button className="primary">Save tax rule</button></form>
          <form className="admin-card" onSubmit={e => saveRule(e, 'deductions')}><h3>Deduction rule</h3><label>Rule name<input name="name" placeholder="Health insurance" required /></label><RuleValueFields /><button className="primary">Save deduction</button></form>
          <form className="admin-card" onSubmit={savePayCode}><h3>Entitlement / pay-code rule</h3><label>Pay code<input name="payCode" placeholder="SICK" required /></label><label>Description<input name="description" required /></label><label>Pay multiplier<input name="multiplier" type="number" min="0" step="0.01" defaultValue="1" required /></label><label className="checkbox-label"><input name="countsTowardOvertime" type="checkbox" /> Counts toward the 40-hour overtime threshold</label><button className="primary">Save pay code</button></form>
        </div>
        <div className="rule-lists"><div><h3>Configured taxes</h3>{taxes.map(t => <span className="rule-chip" key={t.id}>{t.name} · {t.scope}{t.region ? ` / ${t.region}` : ''} · {t.kind === 'PERCENTAGE' ? `${t.value}%` : `$${t.value}`}</span>)}</div>
          <div><h3>Configured deductions</h3>{deductions.map(d => <span className="rule-chip" key={d.id}>{d.name} · {d.kind === 'PERCENTAGE' ? `${d.value}%` : `$${d.value}`}</span>)}</div>
          <div><h3>Configured pay codes</h3>{payCodes.map(code => <span className="rule-chip" key={code.payCode}>{code.payCode} · {code.multiplier}× · {code.countsTowardOvertime ? 'counts toward overtime' : 'separate paid entitlement'}</span>)}</div></div>
        <p className="fineprint">Percentage rules are applied to gross pay; flat rules are withheld once per weekly statement. These configurable rules do not implement jurisdiction-specific tax brackets or tax advice. Approved adjustments require an active pay-code rule; separate entitlement codes do not count toward overtime.</p>
      </section>}
      <footer>Overtime is calculated after 40 hours per week at 1.5× the regular hourly rate. Incentives are excluded from gross pay.</footer>
    </main>
  </div>;
}

function RuleValueFields() {
  return <><label>Calculation<select name="kind"><option value="PERCENTAGE">Percentage of gross pay</option><option value="FLAT">Flat amount per statement</option></select></label><label>Value<input name="value" type="number" min="0" step="0.01" required /></label></>;
}

function RequestList({ requests, actions }) {
  if (!requests.length) return <p className="empty-box">No requests yet.</p>;
  return <div className="request-list">{requests.map(request => <article className="request" key={request.id}>
    <div><strong>{request.employeeId}</strong><span>{request.weekEnding} · {request.payCode} · {request.hours} hours</span><p>{request.explanation}</p></div>
    <div className="request-actions"><span className={`badge ${request.status.toLowerCase()}`}>{request.status}</span>
      {actions && request.status === 'PENDING' && <><button className="approve" onClick={() => actions(request.id, true)}>Approve</button><button className="reject" onClick={() => actions(request.id, false)}>Reject</button></>}</div>
  </article>)}</div>;
}

createRoot(document.getElementById('root')).render(<App />);
