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
    const categorySelect = document.getElementById('categoryFilter');
    const prioritySelect = document.getElementById('priorityFilter');
    const slaSelect = document.getElementById('slaFilter');

    const categoryFilter = categorySelect ? categorySelect.value.toLowerCase() : 'all';
    const priorityFilter = prioritySelect ? prioritySelect.value.toUpperCase() : 'ALL';
    const slaFilter = slaSelect ? slaSelect.value.toUpperCase() : 'ALL';

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

// Xóa bộ lọc trên Màn hình 1 (Empty State action)
function resetTicketFilter() {
    const categorySelect = document.getElementById('categoryFilter');
    const prioritySelect = document.getElementById('priorityFilter');
    const slaSelect = document.getElementById('slaFilter');

    if (categorySelect) categorySelect.value = 'all';
    if (prioritySelect) prioritySelect.value = 'ALL';
    if (slaSelect) slaSelect.value = 'ALL';

    applyTicketFilter();
}

// Xử lý 3 trạng thái UI (Loading, Empty, Error)
function showLoadingState(message) {
    const loader = document.getElementById('uiLoadingState') || document.getElementById('uiStateLoading');
    const content = document.getElementById('ticketTableContainer') || document.getElementById('resultsContainer') || document.getElementById('mainContentArea');
    if (loader) {
        loader.style.display = 'block';
        if (message) {
            const msgEl = loader.querySelector('.loading-text') || loader.querySelector('p');
            if (msgEl) msgEl.textContent = message;
        }
    }
    if (content) {
        content.style.opacity = '0.3';
    }
}

function hideLoadingState() {
    const loader = document.getElementById('uiLoadingState') || document.getElementById('uiStateLoading');
    const content = document.getElementById('ticketTableContainer') || document.getElementById('resultsContainer') || document.getElementById('mainContentArea');
    if (loader) loader.style.display = 'none';
    if (content) content.style.opacity = '1';
}

function showErrorState(message, requestId) {
    const banner = document.getElementById('uiDynamicErrorBanner');
    const msgEl = document.getElementById('uiDynamicErrorMessage');
    const reqEl = document.getElementById('uiDynamicRequestId');
    if (banner) {
        if (msgEl) msgEl.innerText = message || 'Hệ thống đang gặp sự cố. Vui lòng thử lại sau.';
        if (reqEl) reqEl.innerText = requestId || 'N/A';
        banner.style.display = 'flex';
    }
}

// Mở Modal Phân công & Đặt lịch từ dòng dữ liệu trên Màn hình 1
function openAssignModalFromRow(btn) {
    const ticketId = btn.dataset.ticketId || '1';
    const ticketCode = btn.dataset.ticketCode || '—';
    const deviceName = btn.dataset.deviceName || 'iPhone 13';
    const categoryName = btn.dataset.categoryName || 'Màn hình';
    const dueDate = btn.dataset.dueDate || '30/10/2026 08:00';
    const repeatCount = parseInt(btn.dataset.repeatCount || '2', 10);
    const currentTechId = btn.dataset.currentTechId || '';

    // Cập nhật thông tin vào modal
    const modalTitleEl = document.getElementById('modalTicketCodeTitle');
    if (modalTitleEl) modalTitleEl.textContent = ticketCode;

    const modalSummaryEl = document.getElementById('modalSummaryText');
    if (modalSummaryEl) {
        modalSummaryEl.textContent = `Thiết bị: ${deviceName} | Nhóm sự cố: ${categoryName} | Hạn SLA: ${dueDate}`;
    }

    // Set currentTechId vào hidden field
    const currentTechInput = document.getElementById('modalCurrentTechId') || document.getElementById('currentTechId');
    if (currentTechInput) {
        currentTechInput.value = currentTechId;
    }

    // Reset textarea changeReason & error state
    const reasonInput = document.getElementById('changeReason') || document.getElementById('modalChangeReason');
    if (reasonInput) {
        reasonInput.value = '';
        reasonInput.classList.remove('is-invalid');
    }
    const errDiv = document.getElementById('changeReasonError');
    if (errDiv) {
        errDiv.style.display = 'none';
    }

    // Cảnh báo lỗi lặp (US6/FR6 & US5/FR5)
    const repeatAlert = document.getElementById('modalRepeatAlert');
    const repeatCountEl = document.getElementById('modalRepeatCount');
    const repeatCategoryEl = document.getElementById('modalRepeatCategory');
    if (repeatAlert) {
        if (repeatCount >= 2) {
            if (repeatCountEl) repeatCountEl.textContent = repeatCount;
            if (repeatCategoryEl) repeatCategoryEl.textContent = categoryName;
            repeatAlert.style.display = '';
        } else {
            repeatAlert.style.display = 'none';
        }
    }

    // Set action URL form
    const formEl = document.getElementById('modalAssignForm');
    if (formEl) {
        formEl.action = `/tickets/${ticketId}/assign`;
    }

    // Nạp động danh sách KTV đủ điều kiện (QT-08: cùng trung tâm, proficiency >= 3)
    const techSelect = document.getElementById('modalTechnicianSelect');
    if (techSelect && ticketId) {
        fetch(`/tickets/${ticketId}/eligible-technicians`)
            .then(res => res.ok ? res.json() : [])
            .then(techs => {
                if (Array.isArray(techs) && techs.length > 0) {
                    techSelect.innerHTML = techs.map((t, idx) =>
                        `<option value="${t.technicianId}" ${idx === 0 ? 'selected' : ''}>${t.fullName} (Bậc ${t.level} - Đang giữ ${t.activeTicketsCount || 0} phiếu)</option>`
                    ).join('');
                }
            })
            .catch(err => console.warn('Không thể nạp KTV động:', err));
    }

    // Reset cảnh báo trùng lịch
    const conflictAlert = document.getElementById('modalConflictAlert');
    if (conflictAlert) conflictAlert.style.display = 'none';

    // Mở Bootstrap 5 modal
    const assignModalEl = document.getElementById('assignModal');
    if (assignModalEl && window.bootstrap && window.bootstrap.Modal) {
        const modalInstance = bootstrap.Modal.getOrCreateInstance(assignModalEl);
        modalInstance.show();
    }
}

// Kiểm tra cảnh báo trùng lịch hẹn (QT-L4-01)
// Báo trùng lịch chỉ được lấy từ phản hồi lỗi backend (errorCode SCHEDULE_CONFLICT, message của server)
function checkTechnicianScheduleConflict() {
    const conflictAlert = document.getElementById('modalConflictAlert') || document.getElementById('conflictAlert');
    if (conflictAlert) {
        conflictAlert.style.display = 'none';
    }
}

// Bổ sung kiểm tra validation QT-07 khi submit form phân công KTV
document.addEventListener('DOMContentLoaded', () => {
    const formsToValidate = [
        document.getElementById('modalAssignForm'),
        document.getElementById('assignModalForm'),
        document.getElementById('assignStandardForm')
    ];

    formsToValidate.forEach(form => {
        if (!form) return;
        form.addEventListener('submit', function (e) {
            const currentTechInput = form.querySelector('input[name="currentTechId"]') || document.getElementById('modalCurrentTechId') || document.getElementById('currentTechId');
            const currentTechId = currentTechInput ? currentTechInput.value : '';
            const techSelect = form.querySelector('select[name="technicianId"]') || document.getElementById('modalTechnicianSelect') || document.getElementById('technicianId');
            const newTechId = techSelect ? techSelect.value : '';
            const reasonInput = form.querySelector('textarea[name="changeReason"]') || document.getElementById('changeReason') || document.getElementById('modalChangeReason');
            const errDiv = form.querySelector('#changeReasonError') || document.getElementById('changeReasonError');

            // QT-07: Nếu ticket đã có KTV trước đó và chọn đổi sang KTV khác mà nhập dưới 10 ký tự
            if (currentTechId && newTechId && currentTechId !== newTechId) {
                const reasonText = reasonInput ? reasonInput.value.trim() : '';
                if (reasonText.length < 10) {
                    e.preventDefault();
                    e.stopPropagation();
                    if (errDiv) {
                        errDiv.style.display = 'block';
                        errDiv.textContent = 'Quy tắc QT-07: Khi thay đổi Kỹ thuật viên đã được phân công, lý do thay đổi phải có ít nhất 10 ký tự!';
                    }
                    if (reasonInput) {
                        reasonInput.classList.add('is-invalid');
                        reasonInput.focus();
                    }
                    const submitBtn = form.querySelector('button[type="submit"]');
                    if (submitBtn) {
                        submitBtn.classList.remove('btn-loading');
                        submitBtn.innerHTML = submitBtn.dataset.originalText || 'Lưu phân công & Đặt lịch';
                    }
                    return false;
                }
            }

            if (errDiv) errDiv.style.display = 'none';
            if (reasonInput) reasonInput.classList.remove('is-invalid');
        });
    });
});

