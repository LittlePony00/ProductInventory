//
// Created by Nikita Verescshagin on 5/7/26.
//

import Shared
import Foundation

final class SharedVMHolder<State: AnyObject, Event: AnyObject, Action: AnyObject, VM: SharedViewModel<State, Event, Action>>: ObservableObject {
    let viewModel: VM
    @Published private(set) var state: State
    private var disposableHandle: Kotlinx_coroutines_coreDisposableHandle?

    init(viewModel: VM, initialState: State) {
        self.viewModel = viewModel
        self.state = initialState
    }

    func start(onAction: @escaping (Action) -> Void = { _ in }) {
        disposableHandle = FlowWatchUtilsKt.bind(
            state: viewModel.viewState,
            onState: { [weak self] newState in
                if let typedState = newState as? State {
                    DispatchQueue.main.async {
                        self?.state = typedState
                    }
                }
            },
            action: viewModel.viewAction,
            onAction: { action in
                if let typedAction = action as? Action {
                    onAction(typedAction)
                }
            }
        )
    }

    func sendEvent(_ event: Event) {
        viewModel.onEvent(event: event)
    }

    func stop() {
        disposableHandle?.dispose()
        disposableHandle = nil
    }

    deinit {
        stop()
    }
}
