/**
 * JavaScript tương tác cho giao diện điều phối Mekong Mobile Smart CRM.
 */
document.addEventListener('DOMContentLoaded', () => {
    console.log('Smart CRM Technician Scheduling system loaded.');
});

function openScheduleModal(ticketId, ticketCode, centerId) {
    document.getElementById('modalTicketId').value = ticketId;
    document.getElementById('modalTicketCode').innerText = ticketCode;
    
    // Set default appointment time to tomorrow 09:00 AM
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    tomorrow.setHours(9, 0, 0, 0);
    const localIso = new Date(tomorrow.getTime() - tomorrow.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
    document.getElementById('modalAppointmentTime').value = localIso;

    fetchEligibleTechnicians(ticketId, localIso);
    document.getElementById('scheduleModal').style.display = 'flex';
}

function closeScheduleModal() {
    document.getElementById('scheduleModal').style.display = 'none';
}

async function fetchEligibleTechnicians(ticketId, appointmentTime) {
    const select = document.getElementById('modalTechnicianSelect');
    select.innerHTML = '<option value="">Đang tải danh sách kỹ thuật viên khả dụng...</option>';

    try {
        const response = await fetch(`/api/v1/scheduling/eligible-technicians?ticketId=${ticketId}&appointmentTime=${appointmentTime}:00`);
        const result = await response.json();
        
        if (result.success && result.data && result.data.length > 0) {
            select.innerHTML = result.data.map(tech => 
                `<option value="${tech.technicianId}">${tech.fullName} (Mã NV: ${tech.employeeId} - Bậc: ${tech.level})</option>`
            ).join('');
        } else {
            select.innerHTML = '<option value="">Không có kỹ thuật viên phù hợp/trùng lịch</option>';
        }
    } catch (err) {
        console.error('Error fetching technicians:', err);
        select.innerHTML = '<option value="">Lỗi tải danh sách kỹ thuật viên</option>';
    }
}

async function submitSchedule() {
    const ticketId = document.getElementById('modalTicketId').value;
    const technicianId = document.getElementById('modalTechnicianSelect').value;
    const appointmentTime = document.getElementById('modalAppointmentTime').value;

    if (!technicianId) {
        alert('Vui lòng chọn kỹ thuật viên!');
        return;
    }

    try {
        const payload = {
            ticketId: parseInt(ticketId),
            technicianId: parseInt(technicianId),
            appointmentTime: appointmentTime + ':00',
            scheduledBy: 1 // Giả lập user điều phối đăng nhập
        };

        const res = await fetch('/api/v1/scheduling/appointments', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const data = await res.json();
        if (res.ok && data.success) {
            alert('Điều phối kỹ thuật viên thành công (QT-07)!');
            window.location.reload();
        } else {
            alert('Lỗi: ' + (data.message || 'Không thể điều phối kỹ thuật viên'));
        }
    } catch (err) {
        console.error('Lỗi khi gửi điều phối:', err);
        alert('Lỗi kết nối tới hệ thống điều phối');
    }
}

// BƯỚC 4: Trạng thái Đang tải (Loading state) cho các form submit
document.addEventListener('DOMContentLoaded', () => {
    const forms = document.querySelectorAll('form');
    forms.forEach(form => {
        form.addEventListener('submit', function (e) {
            const submitBtn = this.querySelector('button[type="submit"]');
            if (submitBtn && !submitBtn.classList.contains('btn-loading')) {
                const originalText = submitBtn.innerHTML;
                submitBtn.dataset.originalText = originalText;
                submitBtn.classList.add('btn-loading');
                submitBtn.innerHTML = '<span class="spinner"></span> Đang xử lý...';
            }
        });
    });
});

// Bộ lọc danh sách phiếu chưa gán (Màn hình 1)
function applyTicketFilter() {
    const categoryFilter = document.getElementById('categoryFilter') ? document.getElementById('categoryFilter').value.toLowerCase() : 'all';
    const priorityFilter = document.getElementById('priorityFilter') ? document.getElementById('priorityFilter').value.toUpperCase() : 'ALL';
    const slaFilter = document.getElementById('slaFilter') ? document.getElementById('slaFilter').value.toUpperCase() : 'ALL';

    const rows = document.querySelectorAll('.ticket-row');
    let visibleCount = 0;

    rows.forEach(row => {
        const cat = (row.dataset.category || '').toLowerCase();
        const prio = (row.dataset.priority || '').toUpperCase();
        const sla = (row.dataset.sla || '').toUpperCase();

        const matchCat = (categoryFilter === 'all' || cat.includes(categoryFilter));
        const matchPrio = (priorityFilter === 'ALL' || prio === priorityFilter);
        const matchSla = (slaFilter === 'ALL' || sla === slaFilter);

        if (matchCat && matchPrio && matchSla) {
            row.style.display = '';
            visibleCount++;
        } else {
            row.style.display = 'none';
        }
    });

    const emptyFilterRow = document.getElementById('emptyFilterRow');
    if (emptyFilterRow) {
        emptyFilterRow.style.display = (visibleCount === 0 && rows.length > 0) ? '' : 'none';
    }
}
